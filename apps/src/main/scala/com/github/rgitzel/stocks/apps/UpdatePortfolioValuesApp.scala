package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.stocks.influxdb.{InfluxDbForexRepository, InfluxDbPricesRepository, InfluxDbOperations}
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

    val day = TradingDay(4, 1, 2022)

    val desiredCurrency = Currency("CAD")

    val portfolios = Map(
    )

    val data = for {
      prices <- pricesRepository.closingPrices(day)
      rates <- forexRepository.closingRates(day)
    }
    yield (rates, prices)

    data
      .flatMap { case (exchangeRates, pricesForStocks) =>
        val (alreadyInDesiredCurrency, needToBeConverted) = pricesForStocks.partition(_._2.currency == desiredCurrency)
        // add prices in desired currency
        Future.sequence(
          needToBeConverted
            .map { case (stock, price) =>
              val newPrice = price.value * exchangeRates.getOrElse(ConversionCurrencies(price.currency, desiredCurrency), throw new Exception("wtf?"))
              println(s"updating ${stock} with ${newPrice}")
              pricesRepository.updateClosingPrice(day, stock, Price(newPrice, desiredCurrency))
                .map { _ =>
                  (stock, Price(newPrice, desiredCurrency))
                }
            }
        )
          .map { convertedPrices =>
            val pricesInDesiredCurrency = alreadyInDesiredCurrency ++ convertedPrices.toMap
            // figure out the values
            portfolios.map { case (label, portfolio) =>
              new Validator(desiredCurrency).areComplete(portfolio, pricesInDesiredCurrency, exchangeRates) match {
                case None =>
                  val results = new Summarizer(desiredCurrency)
                    .summarize(portfolio, pricesInDesiredCurrency, exchangeRates)
                    .toList
                    .sortBy(_._1.symbol)
                  println(label)
                  results.foreach(println)
                  println()
                  results
                case Some(error) =>
                  println(error)
                  Future.failed(new Exception("failed to validate"))
              }
            }
        }
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
