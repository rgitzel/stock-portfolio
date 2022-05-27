package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.influxdb.MissingTagsException
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.repositories.PricesRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions

import scala.concurrent.{ExecutionContext, Future}

class InfluxDbPricesRepository(influxDb: InfluxDbOperations)
  extends PricesRepository {
  private val databaseName = "stocks"
  private val measurement = "price"

  override def closingPrices(day: TradingDay)(implicit ec: ExecutionContext): Future[Map[Stock,Price]] = {
    val ts = Days.toInstant(day)
    val fluxQuery = Flux.from(databaseName)
      .range(ts.minusMillis(1), ts.plusMillis(1))
      .filter(
        Restrictions.measurement().equal(measurement)
      )
    influxDb.runQuery(fluxQuery){ (timestamp, price, tags) =>
      (tags.get("currency"), tags.get("symbol")) match {
        case (Some(currencyCode), Some(symbol)) =>
          (
            Stock(symbol),
            Price(price, Currency(currencyCode))
          )
        case _ =>
          throw new MissingTagsException(measurement, timestamp, tags.keys, List("currency", "symbol"))
      }
    }.map(_.toMap)
  }

  override def updateClosingPrice(day: TradingDay, stock: Stock, price: Price)(implicit ec: ExecutionContext): Future[Unit] = {
    val point = Point
      .measurement(measurement)
      .addTag("currency", price.currency.code)
      .addTag("symbol", stock.symbol)
      .addField("price", price.value)
      .time(Days.toInstant(day), WritePrecision.NS)

    influxDb.write("stocks", point).map(_ => ())
  }

}
