package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._

import scala.concurrent.Future

trait ForexRepository {
  def closingRates(day: TradingDay): Future[Map[ConversionCurrencies, Double]]
}
