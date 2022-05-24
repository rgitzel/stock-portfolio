package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models.{Price, Stock, TradingDay}

import scala.concurrent.Future

trait PricesRepository {
  def closingPrices(day: TradingDay): Future[Map[Stock, Price]]
}
