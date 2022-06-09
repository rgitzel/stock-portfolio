package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.quicken.transactions.QuickenAccountJournalsRepository
import com.github.rgitzel.stocks.models._
import com.influxdb.client.scala.{InfluxDBClientScala, InfluxDBClientScalaFactory}

import java.io.File
import java.net.URL
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object GetPortfolioApp extends App {
  def influxDbUrl(): URL = new URL("http://192.168.0.17:8086")

  def useInfluxDbClient(influxDBClient: InfluxDBClientScala)(implicit ec: ExecutionContext): Future[_] = {
    val weeks = TradingWeek(TradingDay(10, 31, 2011)).previousWeeks(1)
    weeks.foreach(println)

    new QuickenAccountJournalsRepository(new File("./transactions.txt"))
      .accountJournals()
      .foreach { case journals =>
        weeks.map { week =>
          println()
          println(s"portfolios as of ${week}")

          journals.map(_.accountAsOf(week.friday))
            .foreach{ portfolio =>
              println(portfolio.name)
              portfolio.shareCountsForStocksForCurrencies
                .foreach{ case(currency, holdings) =>
                  if(holdings.size > 0) {
                    println(s"\t${currency}")
                    holdings.foreach{ case(stock, count) => println(s"\t\t${stock.symbol} $count")}
                  }
                }
              println()
            }
        }
      }

    Future.successful(())
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
