package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.influxdb.InfluxDbOperations
import com.github.rgitzel.quicken.transactions.QuickenPortfolioJournalsRepository
import com.github.rgitzel.stocks.influxdb.{InfluxDbForexRepository, InfluxDbPortfolioValueRepository, InfluxDbPricesRepository}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.{ConversionCurrencies, Currency, MonetaryValue, MoneyConverter}
import com.github.rgitzel.stocks.repositories
import com.github.rgitzel.stocks.repositories.{AccountStockValuationRecord, AccountValuationRecord, ForexRepository, PortfolioValuationRecord, PortfolioValueRepository, PricesRepository, WeeklyRecords}
import com.influxdb.client.scala.{InfluxDBClientScala, InfluxDBClientScalaFactory}

import java.io.File
import java.net.URL
import java.time.Instant
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

    // my data is only good going back to 2015 (due to a few years of lost statements)
    val weeks = TradingWeek(TradingDay(5, 1, 2015)).to(TradingWeek.previousWeek())

    println(s"processing from ${weeks.head} to ${weeks.last}")

    transactionsPortfolioRepository.portfolioJournals().flatMap { journals =>
      val currenciesAcrossAllPortfolios = (journals.flatMap(_.currencies) :+ desiredCurrency).distinct

      println(s"loaded ${journals.size} portfolio journals (${journals.map(_.name).mkString(", ")})" +
        s" using ${currenciesAcrossAllPortfolios.size} currencies (${currenciesAcrossAllPortfolios.map(_.code).mkString(", ") })")

      processEachWeek(weeks, journals, forexRepository, pricesRepository, currenciesAcrossAllPortfolios, desiredCurrency)
        .flatMap{ recordsToBeUpdatedForEachWeek =>
          val recordsForWeeksThatHaveResults = recordsToBeUpdatedForEachWeek.filter(_.accountStocks.nonEmpty)

          val recordsCount = recordsForWeeksThatHaveResults.map(week => week.accountStocks.size + week.accounts.size + 1).sum
          println(s"retrieved data for ${recordsForWeeksThatHaveResults.size} weeks (of ${weeks.size} requested)," +
            s" extracting ${recordsCount} records to update")

          portfolioValueRepository.updateValues(recordsForWeeksThatHaveResults)
        }
    }
  }

  private def processEachWeek(
                             weeks: List[TradingWeek],
                             journals: List[PortfolioJournal],
                             forexRepository: ForexRepository,
                             pricesRepository: PricesRepository,
                             currenciesAcrossAllPortfolios: List[Currency],
                             totalsCurrency: Currency
                           )
                           (implicit ec: ExecutionContext): Future[List[WeeklyRecords]] =
    Future.sequence(
      weeks.map { week =>
        resultsForOneWeek(week, journals, forexRepository, pricesRepository, currenciesAcrossAllPortfolios, totalsCurrency)
          .recoverWith { t =>
            println(s"WARNING! skipping ${week} due to errors: ${t.getMessage} ")
            Future.successful(WeeklyRecords.empty(week))
          }
      }
    )

  private def resultsForOneWeek(
                             week: TradingWeek,
                             journals: List[PortfolioJournal],
                             forexRepository: ForexRepository,
                             pricesRepository: PricesRepository,
                             currenciesAcrossAllPortfolios: List[Currency],
                             totalsCurrency: Currency
                           )
                           (implicit ec: ExecutionContext): Future[WeeklyRecords] = {
    forexRepository.weeklyClosingRates(week).flatMap { exchangeRatesForThisWeek =>
      if(exchangeRatesForThisWeek.isEmpty)
        Future.failed(new Exception(s"no exchange rates found for ${week}"))
      else
        pricesRepository.weeklyClosingPrices(week).flatMap { pricesForStocksThisWeek =>
          if(pricesForStocksThisWeek.isEmpty)
            println(s"WARNING: no stock prices found for ${week}")
          Future.fromTry(
            determineRecordsToBeUpdatedForThisWeek(
              week,
              journals.map(_.portfolioAsOf(week.friday)),
              exchangeRatesForThisWeek,
              pricesForStocksThisWeek,
              currenciesAcrossAllPortfolios,
              totalsCurrency
            )
          )
        }
      }
  }

  // TODO: this needs to be broken up....
  private def determineRecordsToBeUpdatedForThisWeek(
                       week: TradingWeek,
                       portfoliosForThisWeek: List[Portfolio],
                       exchangeRatesForThisWeek: Map[ConversionCurrencies, Double],
                       pricesForStocksThisWeek: Map[Stock, MonetaryValue],
                       currenciesAcrossAllPortfolios: List[Currency],
                       totalsCurrency: Currency
                     )(implicit ec: ExecutionContext): Try[WeeklyRecords] = {
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
        val records = portfolioValuations.map { portfolioValuation =>
          val stocksRecords = portfolioValuation.valuesForStocksForCurrency.flatMap { case (currency, holdings) =>
              holdings
                .toList
                .sortBy(_._1.symbol)
                .map { case (stock, value) =>
                  AccountStockValuationRecord(week.friday, portfolioValuation.name, stock, MonetaryValue(value, currency))
                }
            }
          val subtotalsRecords = stocksRecords
            .groupBy(_.value.currency)
            .map{ case (currency, stockRecords) =>
              AccountValuationRecord(
                week.friday,
                portfolioValuation.name,
                MonetaryValue(
                  stockRecords.map(_.value.value).sum,
                  currency
                )
              )
            }
          (stocksRecords, subtotalsRecords)
        }

        val totalsRecord = PortfolioValuationRecord(
          week.friday,
          MonetaryValue(
            records.flatMap(_._2.flatMap(sub => moneyConverter.convert(sub.value, totalsCurrency))).map(_.value).sum,
            totalsCurrency
          )
        )

        Success(repositories.WeeklyRecords(
          week,
          records.flatMap(_._1.toList),
          records.flatMap(_._2.toList),
          totalsRecord
        ))
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
