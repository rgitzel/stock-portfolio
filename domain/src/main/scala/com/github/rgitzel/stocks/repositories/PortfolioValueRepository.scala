package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models.{MonetaryValue, Stock, TradingDay}

import scala.concurrent.{ExecutionContext, Future}

trait PortfolioValueRepository {
  def updateValue(day: TradingDay, portfolioName: String, stock: Stock, value: MonetaryValue)(implicit ec: ExecutionContext): Future[Unit]
}
