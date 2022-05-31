package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._

import scala.concurrent.{ExecutionContext, Future}

trait PortfolioValueRepository {
  def updateValue(day: TradingDay, portfolioName: PortfolioName, stock: Stock, value: MonetaryValue)(implicit ec: ExecutionContext): Future[Unit]
}
