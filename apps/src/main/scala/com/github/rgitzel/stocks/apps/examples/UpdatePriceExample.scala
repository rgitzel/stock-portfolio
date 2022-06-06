package com.github.rgitzel.stocks.apps.examples

import akka.actor.ActorSystem
import com.github.rgitzel.influxdb.InfluxDbOperations
import com.github.rgitzel.stocks.influxdb.InfluxDbPricesRepository
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.{Currency, MonetaryValue}
import example.InfluxDbExample

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object UpdatePriceExample extends InfluxDbExample {

  implicit val system: ActorSystem = ActorSystem()

  def main(args: Array[String]): Unit = {

    val repository = new InfluxDbPricesRepository(new InfluxDbOperations(influxDBClient))

    val results = repository.updateClosingPrice(TradingDay(5, 13, 2022), Stock("GOOGL"), MonetaryValue(101.0, Currency("CAD")))(global)

    Await.result(results, Duration.Inf)

    println("done")
    influxDBClient.close()
    system.terminate()
  }
}
