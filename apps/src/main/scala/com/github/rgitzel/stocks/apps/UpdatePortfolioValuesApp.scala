package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.stocks.influxdb.{InfluxDbForexRepository, InfluxDbOperations, InfluxDbPortfolioValueRepository, InfluxDbPricesRepository}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.{Summarizer, Validator}
import com.influxdb.client.scala.{InfluxDBClientScala, InfluxDBClientScalaFactory}

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

    val lastDay = TradingDay(8, 6, 2021)
    val days = lastDay.preceedingWeeks(6)
    days.foreach(println)

    val desiredCurrency = Currency("CAD")

    val portfolios = Map(
    )

    val perDay: List[Future[String]] = days.map{ day =>
      println()
      println(s"processing ${day}")

      val data = for {
        prices <- pricesRepository.closingPrices(day)
        rates <- forexRepository.closingRates(day)
      }
      yield (rates, prices)

      data.flatMap { case (exchangeRates, pricesForStocks) =>
        val convertedPrices = pricesToUpdate(day, exchangeRates, pricesForStocks, desiredCurrency)
        pricesRepository.updateClosingPrices(day, convertedPrices)
          .flatMap { _ =>
            val pricesInAllCurrencies = pricesForStocks ++ convertedPrices
            new Validator(desiredCurrency).allDataExistsForPortfolios(portfolios, pricesInAllCurrencies, exchangeRates) match {
              case None =>
                val portfolioUpdates = portfolios.flatMap { case (label, portfolio) =>
                  new Summarizer(desiredCurrency)
                    .summarize(portfolio, pricesInAllCurrencies, exchangeRates)
                    .toList
                    .sortBy(_._1.symbol)
                    .map{ case (stock, value) =>
                      (day, label, stock, MonetaryValue(value, desiredCurrency))
                    }
                }
                portfolioUpdates.foreach(println)
                Future.sequence(
                  portfolioUpdates
                    .map { case (day, label, stock, value) =>
                      portfolioValueRepository.updateValue(day, label, stock, value)
                    }
                )
              case Some(error) =>
                Future.failed(new Exception(s"$day - $error"))
            }
        }
      }
        .map(_ => s"success for ${day}")
        .recover(t => s"failed for ${day}: ${t.getMessage}")
    }

    Future.sequence(perDay).map{ outcomes =>
      println()
      outcomes.foreach(println)
    }
  }


  def pricesToUpdate(
                    day: TradingDay,
                      exchangeRates: Map[ConversionCurrencies,Double],
                      pricesForStocks: Map[Stock,MonetaryValue],
                      desiredCurrency: Currency
                    )(implicit ec: ExecutionContext): Map[Stock,MonetaryValue] = {
    val needToBeConverted = pricesForStocks.filter(_._2.currency != desiredCurrency)
    needToBeConverted
      .view.mapValues { price =>
      val rate = exchangeRates.getOrElse(
        ConversionCurrencies(price.currency, desiredCurrency),
        throw new Exception(s"wtf? no conversion from ${price.currency} to ${desiredCurrency} on ${day}")
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
