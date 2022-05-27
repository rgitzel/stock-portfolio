package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models.{Price, Stock, TradingDay}

import scala.concurrent.{ExecutionContext, Future}

trait PricesRepository {
  def closingPrices(day: TradingDay)(implicit ec: ExecutionContext): Future[Map[Stock, Price]]
  def updateClosingPrice(day: TradingDay, stock: Stock, price: Price)(implicit ec: ExecutionContext): Future[Unit]
}
