package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.influxdb.InfluxDbApp
import com.github.rgitzel.quicken.transactions.QuickenAccountJournalsRepository
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.Currency
import com.influxdb.client.scala.{
  InfluxDBClientScala,
  InfluxDBClientScalaFactory
}

import java.io.File
import java.net.URL
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object GetPortfolioJournalApp extends InfluxDbApp {
  def influxDbUrl(): URL = new URL("http://192.168.0.17:8086")

  override def useInfluxDbClient(
      influxDBClient: InfluxDBClientScala
  )(implicit ec: ExecutionContext): Future[_] = {
    import Constants._

    val weeks = List(
      TradingWeek.yearEnd(2021)
//      lastTradingWeek2018
    )
    weeks.foreach(println)

    new QuickenAccountJournalsRepository(rawQuickenFiles)
      .accountJournals()
      .andThen {
        case Failure(t) =>
          println(t.getMessage)
        case Success(journals) =>
          weeks.map { week =>
            println()
            println(s"portfolio journal as of ${week}")

            journals
              //            .filter(_.name.s == "RSP")
              .foreach { journal =>
                println(journal.name)
                journal.activities
                  .filter(_.tradingDay <= week.friday)
                  //                .filter(_.currency == Currency("USD"))
                  .foreach { txn => println(s"\t${txn} ${txn.action.value}") }
                println()
              }
          }
      }
  }
}
