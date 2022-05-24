package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.models.{Currency, TradingDay}

import scala.concurrent.Future

trait ForexRepository {
  def closingRates(day: TradingDay): Future[Map[(Currency,Currency),Double]]
}
