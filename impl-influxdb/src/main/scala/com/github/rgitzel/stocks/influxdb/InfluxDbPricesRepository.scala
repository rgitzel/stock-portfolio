package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.influxdb.{
  InfluxDbOperations,
  MissingTagsException,
  SimplerFluxRecord
}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.{Currency, MonetaryValue}
import com.github.rgitzel.stocks.repositories.PricesRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class InfluxDbPricesRepository(influxDb: InfluxDbOperations)
    extends PricesRepository {
  private val databaseName = "stocks"
  private val measurement = "price"

  override def dailyClosingPrices(
      day: TradingDay
  )(implicit ec: ExecutionContext): Future[Map[Stock, MonetaryValue]] = {
    val ts = TradingDay.toInstant(day)
    runQuery(ts.minusMillis(1), ts.plusMillis(1))
  }

  override def weeklyClosingPrices(
      week: TradingWeek
  )(implicit ec: ExecutionContext): Future[Map[Stock, MonetaryValue]] = {
    val ts = TradingDay.toInstant(week.friday)
    runQuery(ts.minus(7, ChronoUnit.DAYS).plusMillis(1), ts.plusMillis(1))
  }

  override def updateClosingPrice(
      day: TradingDay,
      stock: Stock,
      price: MonetaryValue
  )(implicit ec: ExecutionContext): Future[Unit] = {
    val point = Point
      .measurement(measurement)
      .addTag("currency", price.currency.code)
      .addTag("symbol", stock.symbol)
      .addField("price", price.value)
      .time(TradingDay.toInstant(day), WritePrecision.NS)

    influxDb.write(databaseName, point).map(_ => ())
  }

  // ==================================

  private def runQuery(start: Instant, end: Instant)(implicit
      ec: ExecutionContext
  ): Future[Map[Stock, MonetaryValue]] = {
    val fluxQuery = Flux
      .from(databaseName)
      .range(start, end)
      .filter(
        Restrictions.measurement().equal(measurement)
      )
    influxDb
      .runQuery(fluxQuery) { case SimplerFluxRecord(timestamp, price, tags) =>
        (tags.get("currency"), tags.get("symbol")) match {
          case (Some(currencyCode), Some(symbol)) =>
            (
              Stock(symbol),
              MonetaryValue(price, Currency(currencyCode))
            )
          case _ =>
            throw new MissingTagsException(
              measurement,
              timestamp,
              tags.keys,
              List("currency", "symbol")
            )
        }
      }
      .map(_.toMap)
  }
}
