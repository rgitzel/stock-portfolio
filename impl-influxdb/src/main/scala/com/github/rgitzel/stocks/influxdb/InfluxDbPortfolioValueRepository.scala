package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.influxdb.MissingTagsException
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.MonetaryValue
import com.github.rgitzel.stocks.repositories.{PortfolioValueRepository, PricesRepository}
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions

import scala.concurrent.{ExecutionContext, Future}

class InfluxDbPortfolioValueRepository(influxDb: InfluxDbOperations)
  extends PortfolioValueRepository {
  private val databaseName = "stocks"
  private val measurement = "portfolio"

  override def updateValue(day: TradingDay, portfolioName: PortfolioName, stock: Stock, value: MonetaryValue)(implicit ec: ExecutionContext): Future[Unit] = {
    val point = Point
      .measurement(measurement)
      .addTag("name", portfolioName.s)
      .addTag("currency", value.currency.code)
      .addTag("symbol", stock.symbol)
      .addField("value", value.value)
      .time(TradingDay.toInstant(day), WritePrecision.NS)

    influxDb.write(databaseName, point).map(_ => ())
  }

}
