package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.quicken.transactions.QuickenTransactionsPortfolioRepository
import com.github.rgitzel.stocks.influxdb.{InfluxDbForexRepository, InfluxDbOperations, InfluxDbPortfolioValueRepository, InfluxDbPricesRepository}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.{ConversionCurrencies, Currency, MonetaryValue, MoneyConverter}
import com.influxdb.client.scala.{InfluxDBClientScala, InfluxDBClientScalaFactory}

import java.io.File
import java.net.URL
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object UpdatePortfolioValuesApp extends App {
  def influxDbUrl(): URL = new URL("http://192.168.0.17:8086")

  def useInfluxDbClient(influxDBClient: InfluxDBClientScala)(implicit ec: ExecutionContext): Future[_] = {
    val influxDb = new InfluxDbOperations(influxDBClient)
    val pricesRepository = new InfluxDbPricesRepository(influxDb)
    val forexRepository = new InfluxDbForexRepository(influxDb)
    val portfolioValueRepository = new InfluxDbPortfolioValueRepository(influxDb)
    val transactionsPortfolioRepository = new QuickenTransactionsPortfolioRepository(new File("./transactions.txt"))

    val weeks = TradingWeek(TradingDay(5, 27, 2022)).previousWeeks(11 * 52)
    weeks.foreach(println)

    val desiredCurrency = Currency("CAD")

    transactionsPortfolioRepository.portfolioTransactions()
      .flatMap { journals =>
        val resultsByWeek: List[Future[String]] = weeks.map { week =>
          println()
          println(s"processing ${week}")

          val portfolios = journals.map(_.portfolioAsOf(week.lastDay))

          pricesRepository.closingPrices(week).flatMap{ retrievedPricesForStocks =>

            forexRepository.closingRates(week.lastDay).flatMap{ exchangeRatesx =>
              val moneyConverter = new MoneyConverter(exchangeRatesx)

              // somehow need a better way to account for having Apple in both currencies
              val pricesForStocks = retrievedPricesForStocks ++ appleKludge(moneyConverter, retrievedPricesForStocks)

              Portfolio.checkForMissingPrices(portfolios, pricesForStocks) match {
                case Nil =>
                  val portfolioValuations = portfolios.map(Portfolio.valuate(_, pricesForStocks))
                  portfolioValueRepository.updateValues(week, portfolioValuations)
                case errors =>
                  // TODO: better combination of errors
                  Future.failed(new Exception(s"$week - ${errors.mkString(", ")}"))
              }
            }
          }
            .map(_ => s"success for ${week}")
            .recover(t => s"failed for ${week}: ${t.getMessage}")
        }

        Future.sequence(resultsByWeek).map { outcomes =>
          println()
          outcomes.foreach(println)
        }
      }
  }

  def appleKludge(moneyConverter: MoneyConverter, retrievedPricesForStocks: Map[Stock,MonetaryValue]) = {
    val apple = Stock("AAPL")
    val fakeCanadianApple = Stock("AAPLCAD")

    retrievedPricesForStocks.get(apple).flatMap { usPrice =>
      moneyConverter.convert(usPrice, Currency("CAD")).map { cadPrice =>
        Map(fakeCanadianApple -> cadPrice)
      }
    }.getOrElse(Map())
  }

  def pricesToUpdate(
                    week: TradingWeek,
                      exchangeRates: Map[ConversionCurrencies,Double],
                      pricesForStocks: Map[Stock,MonetaryValue],
                      desiredCurrency: Currency
                    )(implicit ec: ExecutionContext): Map[Stock,MonetaryValue] = {
    val needToBeConverted = pricesForStocks.filter(_._2.currency != desiredCurrency)
    needToBeConverted
      .view.mapValues { price =>
      val rate = exchangeRates.getOrElse(
        ConversionCurrencies(price.currency, desiredCurrency),
        throw new Exception(s"wtf? no conversion from ${price.currency} to ${desiredCurrency} for week ending ${week.lastDay}")
      )
      MonetaryValue(price.value * rate, desiredCurrency)
    }
      .toMap
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
    if(!influxDBClient.ping) {
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
