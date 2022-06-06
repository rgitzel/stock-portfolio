package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.quicken.transactions.QuickenPortfolioJournalsRepository
import com.github.rgitzel.stocks.influxdb.{InfluxDbForexRepository, InfluxDbOperations, InfluxDbPortfolioValueRepository, InfluxDbPricesRepository}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.{ConversionCurrencies, Currency, MonetaryValue, MoneyConverter}
import com.github.rgitzel.stocks.repositories.{ForexRepository, PortfolioValuationRecord, PortfolioValueRepository, PricesRepository}
import com.influxdb.client.scala.{InfluxDBClientScala, InfluxDBClientScalaFactory}

import java.io.File
import java.net.URL
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object UpdatePortfolioValuesApp extends App {
  def influxDbUrl(): URL = new URL("http://192.168.0.17:8086")

  def useInfluxDbClient(influxDBClient: InfluxDBClientScala)(implicit ec: ExecutionContext): Future[_] = {
    val influxDb = new InfluxDbOperations(influxDBClient)
    val pricesRepository = new InfluxDbPricesRepository(influxDb)
    val forexRepository = new InfluxDbForexRepository(influxDb)
    val portfolioValueRepository = new InfluxDbPortfolioValueRepository(influxDb)
    val transactionsPortfolioRepository = new QuickenPortfolioJournalsRepository(new File("./transactions.txt"))

    val desiredCurrency = Currency("CAD")

    // my data is only good going back seven years
    val numberOfWeeks = 7 * 52
    val weeks = TradingWeek(TradingDay(6, 3, 2022)).previousWeeks(numberOfWeeks)

    transactionsPortfolioRepository.portfolioJournals().flatMap { journals =>
      val currenciesAcrossAllPortfolios = (journals.flatMap(_.currencies) :+ desiredCurrency).distinct

      println(s"loaded ${journals.size} portfolio journals (${journals.map(_.name).mkString(", ")})" +
        s" using ${currenciesAcrossAllPortfolios.size} currencies (${currenciesAcrossAllPortfolios.map(_.code).mkString(", ") })")

      Future.sequence(resultsByWeek(weeks, journals, forexRepository, pricesRepository, currenciesAcrossAllPortfolios))
        .flatMap{ recordsForWeeks =>
          val recordsForWeeksThatHaveResults = recordsForWeeks.filter(_._2.nonEmpty)

          val allRecords = recordsForWeeksThatHaveResults.flatMap(_._2)
          println(s"retrieved data for ${recordsForWeeksThatHaveResults.size} weeks (of ${numberOfWeeks} requested)," +
            s" extracting ${allRecords.size} records to update")

          portfolioValueRepository.update(allRecords)
            .andThen {
              case Success(_) =>
                println(s"successfully wrote ${allRecords.size} records")
              case Failure(t) =>
                println(s"failed to write results: ${t.getMessage}")
            }
        }
    }
  }

  private def resultsByWeek(
                             weeks: List[TradingWeek],
                             journals: List[PortfolioJournal],
                             forexRepository: ForexRepository,
                             pricesRepository: PricesRepository,
                             currenciesAcrossAllPortfolios: List[Currency]
                           )
                           (implicit ec: ExecutionContext): List[Future[(TradingWeek,List[PortfolioValuationRecord])]] =
    weeks.map { week =>
      forexRepository.weeklyClosingRates(week).flatMap { exchangeRatesForThisWeek =>
        pricesRepository.weeklyClosingPrices(week).flatMap { pricesForStocksThisWeek =>
          Future.fromTry(
            recordsForThisWeek(
              week,
              journals.map(_.portfolioAsOf(week.lastDay)),
              exchangeRatesForThisWeek,
              pricesForStocksThisWeek,
              currenciesAcrossAllPortfolios
            )
              .recoverWith{ t =>
                // TODO: is this the right place to drop the error? in particular, when missing data for a given week
                println(s"WARNING! skipping ${week} due errors: ${t.getMessage} ")
                Success(List[PortfolioValuationRecord]())
              }
              .map{ records =>
//                println(s"built up ${records.size} records for ${week}")
                (week, records)
              }
          )
        }
      }
    }

  // TODO: obviously this needs to be broken up....
  private def recordsForThisWeek(
                       week: TradingWeek,
                       portfoliosForThisWeek: List[Portfolio],
                       exchangeRatesForThisWeek: Map[ConversionCurrencies, Double],
                       pricesForStocksThisWeek: Map[Stock, MonetaryValue],
                       currenciesAcrossAllPortfolios: List[Currency]
                     )(implicit ec: ExecutionContext): Try[List[PortfolioValuationRecord]] = {
    val moneyConverter = new MoneyConverter(exchangeRatesForThisWeek)

    // we want the closing price of each stock in _all_ currencies (it's possible to have, say,
    //  AAPL in a Canadian dollar account)
    val closingPrices = pricesForStocksThisWeek.flatMap { case (stock, publishedMonetaryValue) =>
      currenciesAcrossAllPortfolios.flatMap { currency =>
        moneyConverter.convert(publishedMonetaryValue, currency).map(ClosingPrice(stock, _))
      }
    }.toList

    Portfolio.checkForMissingPrices(portfoliosForThisWeek, closingPrices) match {
      case Nil =>
        val portfolioValuations = portfoliosForThisWeek.map(PortfolioValuation(_, closingPrices))
        Success(portfolioValuations.flatMap { portfolioValuation =>
          portfolioValuation.valuesForStocksForCurrency.flatMap { case (currency, holdings) =>
            holdings
              .toList
              .sortBy(_._1.symbol)
              .map { case (stock, value) =>
                PortfolioValuationRecord(week.lastDay, portfolioValuation.name, stock, MonetaryValue(value, currency))
              }
          }
        })
      case errors =>
        // TODO: better combination of errors
        Failure(new Exception(s"$week - ${errors.mkString(", ")}"))
    }
  }

  // =====================================
  // =====================================
  // =====================================
  // =====================================

  // oh for $!$%@#% sake, I've hit the same issue with all but hte first Future disappearing
  //  somewhere... I had thought I finally solved it about a month before I left, but... I didn't
  //  take notes.  Arg.  (In retrospect, could I have archived all my private Slacks?)
  // I'll ping Max, but in the meantime will copy this here

  // =======================================

  implicit val system: ActorSystem = ActorSystem()

  val influxDBClient = InfluxDBClientScalaFactory
    .create(influxDbUrl().toExternalForm, "".toCharArray, "foo")

  try {
    if (!influxDBClient.ping) {
      println("failed to connect to InfluxDb!")
    }
    else {
      println("connected to InfluxDb successfully")

      val result = useInfluxDbClient(influxDBClient)(global)
        .andThen {
          case Success(_) =>
            println("finished successfully")
          case Failure(t) =>
            println(s"failed on ${t.getMessage}")
        }(global)

      Await.result(result, 10.seconds)
    }
  }
  finally {
    influxDBClient.close()
    system.terminate()
  }
}
