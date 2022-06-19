package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.influxdb.{
  InfluxDbOperations,
  MissingTagsException,
  SimplerFluxRecord
}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.{ConversionCurrencies, Currency}
import com.github.rgitzel.stocks.repositories.ForexRepository
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class InfluxDbForexRepository(influxdb: InfluxDbOperations)
    extends ForexRepository {
  private val databaseName = "stocks"
  private val measurement = "forex"

  override def dailyClosingRates(day: TradingDay)(implicit
      ec: ExecutionContext
  ): Future[Map[ConversionCurrencies, Double]] = {
    val ts = TradingDay.toInstant(day)
    runQuery(ts.minusMillis(1), ts.plusMillis(1))
  }

  override def weeklyClosingRates(week: TradingWeek)(implicit
      ec: ExecutionContext
  ): Future[Map[ConversionCurrencies, Double]] = {
    val ts = TradingDay.toInstant(week.friday)
    runQuery(ts.minus(7, ChronoUnit.DAYS).plusMillis(1), ts.plusMillis(1))
  }

  // ===================================

  private def runQuery(start: Instant, end: Instant)(implicit
      ec: ExecutionContext
  ): Future[Map[ConversionCurrencies, Double]] = {
    val fluxQuery = Flux
      .from(databaseName)
      .range(start, end)
      .filter(
        Restrictions.measurement().equal(measurement)
      )
    influxdb
      .runQuery(fluxQuery) { case SimplerFluxRecord(timestamp, rate, tags) =>
        (tags.get("from"), tags.get("to")) match {
          case (Some(from), Some(to)) =>
            (ConversionCurrencies(Currency(from), Currency(to)), rate)
          case _ =>
            throw new MissingTagsException(
              measurement,
              timestamp,
              tags.keys,
              List("from", "to")
            )
        }
      }
      .map(_.toMap)
  }
}
