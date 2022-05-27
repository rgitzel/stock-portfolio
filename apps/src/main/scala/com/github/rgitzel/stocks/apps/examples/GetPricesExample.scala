package com.github.rgitzel.stocks.apps.examples

import akka.actor.ActorSystem
import com.github.rgitzel.stocks.influxdb.{InfluxDbPricesRepository, InfluxDbOperations}
import com.github.rgitzel.stocks.models.TradingDay
import example.InfluxDbExample

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object GetPricesExample extends InfluxDbExample {

  implicit val system: ActorSystem = ActorSystem()

  def main(args: Array[String]): Unit = {

    val repository = new InfluxDbPricesRepository(new InfluxDbOperations(influxDBClient))

    val prices = repository.closingPrices(TradingDay(5, 20, 2022))(global)

    Await.result(prices, Duration.Inf).foreach(println)

    influxDBClient.close()
    system.terminate()
  }
}
