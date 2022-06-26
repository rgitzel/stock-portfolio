package com.github.rgitzel.stocks.apps

import com.github.rgitzel.influxdb.InfluxDbApp
import com.github.rgitzel.quicken.transactions.QuickenAccountJournalsRepository
import com.github.rgitzel.stocks.models._
import com.influxdb.client.scala.InfluxDBClientScala

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GetPortfolioApp extends InfluxDbApp {
  def influxDbUrl(): URL = new URL("http://192.168.0.17:8086")

  override def useInfluxDbClient(
      influxDBClient: InfluxDBClientScala
  )(implicit ec: ExecutionContext): Future[_] = {
    import Constants._

    val weeks = List(
      TradingWeek.yearEnd(2021)
      //      lastTradingWeek2017,
      //      lastTradingWeek2018,
      //      lastTradingWeek2019
    )
//    weeks.foreach(println)

    new QuickenAccountJournalsRepository(rawQuickenFiles)
      .accountJournals()
      .andThen {
        case Failure(t) =>
          println(t.getMessage)
        case Success(journals) =>
          weeks.map { week =>
            println()
            println(s"accounts as of ${week}")

            journals
              //            .filter(_.name.s == "RSP")
              .map { journal =>
                val account = journal.accountAsOf(week.friday)
                println(account.name)
                account.holdingsForCurrencies.foreach {
                  case (currency, holdings) =>
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
  }
}
