package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.quicken.transactions.QuickenTransactionsPortfolioRepository
import com.github.rgitzel.stocks.influxdb.{InfluxDbForexRepository, InfluxDbOperations, InfluxDbPortfolioValueRepository, InfluxDbPricesRepository}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.{PortfolioValuator, Validator}
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

    val weeks = TradingWeek(TradingDay(5, 27, 2022)).previousWeeks(5 * 52)
    weeks.foreach(println)

    val desiredCurrency = Currency("CAD")

    new QuickenTransactionsPortfolioRepository(new File("./transactions.txt"))
      .portfolioTransactions()
      .flatMap { case journals =>

        val resultsByWeek: List[Future[String]] = weeks.map { week =>
          println()
          println(s"processing ${week}")

          val data = for {
            prices <- pricesRepository.closingPrices(week)
            rates <- forexRepository.closingRates(week)
          }
          yield (rates, prices)

          val portfolios = journals.map(_.portfolioAsOf(week.lastDay))

          data.flatMap { case (exchangeRates, pricesForStocks) =>
            val convertedPrices = pricesToUpdate(week, exchangeRates, pricesForStocks, desiredCurrency)
            // do we really need to _store_ the converted prices? pretty easy to convert on the fly...
            //        pricesRepository.updateClosingPrices(day, convertedPrices)
            //          .flatMap { _ =>
            val pricesInAllCurrencies = pricesForStocks ++ convertedPrices
            new Validator(desiredCurrency).problemsWithRequiredDataForPortfolios(portfolios, pricesInAllCurrencies, exchangeRates) match {
              case Nil =>
                val portfolioValuations = portfolios.map { portfolio =>
                  new PortfolioValuator(desiredCurrency).valuate(portfolio, pricesInAllCurrencies, exchangeRates)
                }
                // TODO: this should go in the repository class
                val combinedPortfolioUpdates = portfolioValuations.flatMap { portfolioValuation =>
                  portfolioValuation.valuesForStocks.toList
                    .sortBy(_._1.symbol)
                    .map { case (stock, value) =>
                      (week, portfolioValuation.name, stock, MonetaryValue(value, desiredCurrency))
                    }
                }
                combinedPortfolioUpdates.foreach(println)
                Future.sequence(
                  combinedPortfolioUpdates
                    .map { case (week, portfolioName, stock, value) =>
                      portfolioValueRepository.updateValue(week.lastDay, portfolioName, stock, value)
                    }
                )
              case errors =>
                // TODO: better combination of errors
                Future.failed(new Exception(s"$week - ${errors.mkString(", ")}"))
            }
            //        }
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
