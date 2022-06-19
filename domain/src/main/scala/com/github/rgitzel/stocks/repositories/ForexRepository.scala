package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.ConversionCurrencies

import scala.concurrent.{ExecutionContext, Future}

trait ForexRepository {
  def dailyClosingRates(day: TradingDay)(implicit
      ec: ExecutionContext
  ): Future[Map[ConversionCurrencies, Double]]
  def weeklyClosingRates(week: TradingWeek)(implicit
      ec: ExecutionContext
  ): Future[Map[ConversionCurrencies, Double]]
}
