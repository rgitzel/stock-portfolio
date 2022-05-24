package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.repositories.ForexRepository
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions

import scala.concurrent.{ExecutionContext, Future}

class InfluxDbForexRepository(influxdb: InfluxDbQueryRunner)(implicit ec: ExecutionContext)
  extends ForexRepository {
  private val databaseName = "stocks"

  override def closingRates(day: TradingDay): Future[Map[ConversionCurrencies,Double]] = {
    val ts = Days.toInstant(day)
    val fluxQuery = Flux.from(databaseName)
      .range(ts.minusMillis(1), ts.plusMillis(1))
      .filter(
        Restrictions.measurement().equal("forex")
      )

    influxdb.run(fluxQuery){ (_, rate, tags) =>
      (tags.get("from"), tags.get("to")) match {
        case (Some(from), Some(to)) =>
          (
            ConversionCurrencies(
              ThreeLetterCurrencyCodes.toCurrency(from),
              ThreeLetterCurrencyCodes.toCurrency(to)
            ),
            rate
          )
        case _ =>
          throw new Exception("wtf?")
      }
    }.map(_.toMap)
  }
}
