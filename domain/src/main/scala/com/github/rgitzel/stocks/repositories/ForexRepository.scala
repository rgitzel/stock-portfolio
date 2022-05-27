package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._

import scala.concurrent.{ExecutionContext, Future}

trait ForexRepository {
  def closingRates(day: TradingDay)(implicit ec: ExecutionContext): Future[Map[ConversionCurrencies, Double]]
}
