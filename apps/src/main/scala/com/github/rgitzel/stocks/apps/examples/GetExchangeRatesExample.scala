package com.github.rgitzel.stocks.apps.examples

import akka.actor.ActorSystem
import com.github.rgitzel.influxdb.InfluxDbOperations
import com.github.rgitzel.stocks.influxdb.InfluxDbForexRepository
import com.github.rgitzel.stocks.models.{TradingDay, TradingWeek}
import example.InfluxDbExample

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object GetExchangeRatesExample extends InfluxDbExample {

  implicit val system: ActorSystem = ActorSystem()

  def main(args: Array[String]): Unit = {

    val repository = new InfluxDbForexRepository(
      new InfluxDbOperations(influxDBClient)
    )

    val day = TradingDay(4, 15, 2022)

    val results = for {
      byDay <- repository.dailyClosingRates(day)
      byWeek <- repository.weeklyClosingRates(TradingWeek(day))
    } yield (byDay, byWeek)

    val (daily, weekly) = Await.result(results, Duration.Inf)
    println("day")
    daily.foreach(println)
    println("week")
    weekly.foreach(println)

    influxDBClient.close()
    system.terminate()
  }
}
