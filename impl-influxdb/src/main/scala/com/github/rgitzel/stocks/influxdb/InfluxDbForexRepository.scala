package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.influxdb.MissingTagsException
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.repositories.ForexRepository
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions

import scala.concurrent.{ExecutionContext, Future}

class InfluxDbForexRepository(influxdb: InfluxDbOperations)
  extends ForexRepository {
  private val databaseName = "stocks"
  private val measurement = "forex"

  override def closingRates(day: TradingDay)(implicit ec: ExecutionContext): Future[Map[ConversionCurrencies,Double]] = {
    val ts = Days.toInstant(day)
    val fluxQuery = Flux.from(databaseName)
      .range(ts.minusMillis(1), ts.plusMillis(1))
      .filter(
        Restrictions.measurement().equal(measurement)
      )

    influxdb.runQuery(fluxQuery){ (timestamp, rate, tags) =>
      (tags.get("from"), tags.get("to")) match {
        case (Some(from), Some(to)) =>
          (ConversionCurrencies(Currency(from), Currency(to)), rate)
        case _ =>
          throw new MissingTagsException(measurement, timestamp, tags.keys, List("from", "to"))
      }
    }
    .map(_.toMap)
  }
}
