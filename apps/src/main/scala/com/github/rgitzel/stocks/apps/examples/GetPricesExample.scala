package com.github.rgitzel.stocks.apps.examples

import akka.actor.ActorSystem
import com.github.rgitzel.stocks.influxdb.{InfluxDbOperations, InfluxDbPricesRepository}
import com.github.rgitzel.stocks.models.{TradingDay, TradingWeek}
import example.InfluxDbExample

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object GetPricesExample extends InfluxDbExample {

  implicit val system: ActorSystem = ActorSystem()

  def main(args: Array[String]): Unit = {

    val repository = new InfluxDbPricesRepository(new InfluxDbOperations(influxDBClient))

    // tricky examples are Good Friday (no prices) and Christmas Eve (no US prices);
    //  we should get fewer (or no) prices for the day, but should for the week
//    val day = TradingDay(4, 15, 2022)
//    val day = TradingDay(12, 24, 2021)
    val day = TradingDay(5, 27, 2022)

    val f = repository.closingPrices(day).flatMap{ daily =>
      repository.closingPrices(TradingWeek(day)).map{ weekly =>
        println(s"day (${daily.size})")
        daily.toList.sortBy(_._1.symbol).foreach(println)
        println(s"week (${weekly.size})")
        weekly.toList.sortBy(_._1.symbol).foreach(println)
      }
    }
    Await.result(f, Duration.Inf)

// I'd use for-comp originally, but... it's so much less-readable when returning multiple values
//    val results = for {
//      byDay <- repository.closingPrices(day)
//      byWeek <- repository.closingPrices(TradingWeek(day))
//    }
//      yield (byDay, byWeek)
//
//    val (daily, weekly) = Await.result(results, Duration.Inf)
//    println(s"day (${daily.size})")
//    daily.foreach(println)
//    println(s"week (${weekly.size})")
//    weekly.foreach(println)

    influxDBClient.close()
    system.terminate()
  }
}
