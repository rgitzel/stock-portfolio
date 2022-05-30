package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._

import scala.concurrent._

trait PricesRepository {
  def closingPrices(day: TradingDay)(implicit ec: ExecutionContext): Future[Map[Stock, MonetaryValue]]
  def closingPrices(week: TradingWeek)(implicit ec: ExecutionContext): Future[Map[Stock, MonetaryValue]]

  def updateClosingPrice(day: TradingDay, stock: Stock, price: MonetaryValue)(implicit ec: ExecutionContext): Future[Unit]

  // =======================

  // TODO: _this_ should be implemented, and the above implemented by calling this... the `Future.sequence` loses
  //  any particular or multiple failures
  def updateClosingPrices(day: TradingDay, pricesForStocks: Map[Stock,MonetaryValue])(implicit ec: ExecutionContext): Future[Unit] =
    Future.sequence(pricesForStocks.map{ case (stock, price) => updateClosingPrice(day, stock, price) }).map(_ => ())
}
