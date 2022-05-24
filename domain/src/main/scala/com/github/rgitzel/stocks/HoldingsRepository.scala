package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.models.{Stock, TradingDay}

import scala.concurrent.Future

trait HoldingsRepository {
  def holdings(day: TradingDay): Future[Map[Stock,Double]]
}
