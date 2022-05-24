package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.repositories.PricesRepository
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions

import scala.concurrent.{ExecutionContext, Future}

class InfluxDbPricesRepository(influxdb: InfluxDbQueryRunner)(implicit ec: ExecutionContext)
  extends PricesRepository {
  private val databaseName = "stocks"

  override def closingPrices(day: TradingDay): Future[Map[Stock,Price]] = {
    val ts = Days.toInstant(day)
    val fluxQuery = Flux.from(databaseName)
      .range(ts.minusMillis(1), ts.plusMillis(1))
      .filter(
        Restrictions.measurement().equal("price")
      )

    influxdb.run(fluxQuery){ (timestamp, price, tags) =>
      (tags.get("currency"), tags.get("symbol")) match {
        case (Some(currencyCode), Some(symbol)) =>
          (
            Stock(symbol),
            Price(price, ThreeLetterCurrencyCodes.toCurrency(currencyCode))
          )
        case _ =>
          throw new Exception("wtf?")
      }
    }.map(_.toMap)
  }
}
