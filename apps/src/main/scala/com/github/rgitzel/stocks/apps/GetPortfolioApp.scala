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
    import Constants._

    val weeks = List(
      lastTradingWeek2017,
      lastTradingWeek2018,
      lastTradingWeek2019
    )
    weeks.foreach(println)

    new QuickenAccountJournalsRepository(rawQuickenFiles)
      .accountJournals()
      .andThen {
        case Failure(t) => println(t.getMessage)
      }
      .foreach { case journals =>
        weeks.map { week =>
          println()
          println(s"accounts as of ${week}")

          journals
            .filter(_.name.s == "RSP")
            .map { journal =>
              val account = journal.accountAsOf(week.friday)
              println(account.name)
              account.holdingsForCurrencies.foreach{ case(currency, holdings) =>
                println(s"\t${currency}")
                println(s"\t\tcash ${holdings.cash}")
                holdings.stocks.foreach { case (stock, count) =>
                  println(s"\t\t${stock.symbol} $count")
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
