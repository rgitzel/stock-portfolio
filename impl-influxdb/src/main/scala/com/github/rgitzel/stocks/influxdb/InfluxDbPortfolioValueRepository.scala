package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.influxdb.InfluxDbOperations
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.repositories._
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point

import scala.concurrent.{ExecutionContext, Future}

class InfluxDbPortfolioValueRepository(influxDb: InfluxDbOperations)
  extends PortfolioValueRepository {
  private val databaseName = "stocks"
  private val measurement = "portfolio"

  override def update(records: List[PortfolioValuationRecord])(implicit ec: ExecutionContext): Future[Unit] = {
    val points = records.map{ record =>
      Point
        .measurement(measurement)
        .addTag("name", record.portfolioName.s)
        .addTag("currency", record.value.currency.code)
        .addTag("symbol", record.stock.symbol)
        .addField("value", record.value.value)
        .time(TradingDay.toInstant(record.day), WritePrecision.NS)
    }

    influxDb.write(databaseName, points).map(_ => ())
  }

}
