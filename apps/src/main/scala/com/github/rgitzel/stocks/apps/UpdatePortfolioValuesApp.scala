package com.github.rgitzel.stocks.apps

import akka.actor.ActorSystem
import com.github.rgitzel.influxdb.InfluxDbOperations
import com.github.rgitzel.quicken.transactions.QuickenAccountJournalsRepository
import com.github.rgitzel.stocks.PortfolioCalculator
import com.github.rgitzel.stocks.accounts.AccountJournal
import com.github.rgitzel.stocks.influxdb.{
  InfluxDbForexRepository,
  InfluxDbPortfolioValueRepository,
  InfluxDbPricesRepository
}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.Currency
import com.github.rgitzel.stocks.repositories.WeeklyRecords
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

object UpdatePortfolioValuesApp extends App {
  def influxDbUrl(): URL = new URL("http://192.168.0.17:8086")

  def useInfluxDbClient(
      influxDBClient: InfluxDBClientScala
  )(implicit ec: ExecutionContext): Future[Unit] = {
    val desiredCurrency = Currency("CAD")

    // my data is only good going back to 2015 (due to a few years of lost statements)
    val weeks = TradingWeek(5, 1, 2015).to(TradingWeek.mostRecent())
    println(s"processing from ${weeks.head} to ${weeks.last}")

    val influxDb = new InfluxDbOperations(influxDBClient)
    val pricesRepository = new InfluxDbPricesRepository(influxDb)
    val forexRepository = new InfluxDbForexRepository(influxDb)
    val portfolioValueRepository = new InfluxDbPortfolioValueRepository(
      influxDb
    )
    val transactionsPortfolioRepository = new QuickenAccountJournalsRepository(
      Constants.rawQuickenFiles
    )

    val portfolioCalculator = new PortfolioCalculator(
      forexRepository,
      pricesRepository,
      desiredCurrency
    )

    transactionsPortfolioRepository.accountJournals().flatMap {
      accountJournals =>
        // something like 'loaded 4 portfolio journals: INVEST (USD), LIRA (CAD, USD), RSP (CAD, USD), TFSA (CAD, USD)'
        val journalsStrings = accountJournals
          .map { journal =>
            s"${journal.name} (${journal.currencies.map(_.code).sorted.mkString(", ")})"
          }
          .mkString(", ")
        println(
          s"loaded ${accountJournals.size} portfolio journals: ${journalsStrings}"
        )

        portfolioCalculator
          .valuate(weeks, accountJournals)
          .flatMap { weeklyValuations =>
            // turn them into something we can write out to the repository
            val weeklyRecords = weeklyValuations.map { case (week, valuation) =>
              WeeklyRecords.buildRecords(week, valuation, desiredCurrency)
            }
            val recordsCount = weeklyRecords
              .map(weeklyRecord =>
                weeklyRecord.accountStocks.size + weeklyRecord.accounts.size + 1
              )
              .sum
            println(
              s"retrieved data for ${weeklyRecords.size} weeks (of ${weeks.size} requested)," +
                s" and extracted ${recordsCount} records to be updated"
            )

            // and write them out
            portfolioValueRepository
              .updateValues(weeklyRecords)
              .map(totalRecords => println(s"wrote ${totalRecords} records"))
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
    if (!influxDBClient.ping) {
      println("failed to connect to InfluxDb!")
    } else {
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
  } finally {
    influxDBClient.close()
    system.terminate()
  }
}
