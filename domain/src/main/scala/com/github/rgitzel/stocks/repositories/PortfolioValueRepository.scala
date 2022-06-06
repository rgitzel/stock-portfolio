package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.MonetaryValue

import scala.concurrent.{ExecutionContext, Future}

// TODO: what's a better domain name for this?
final case class PortfolioValuationRecord(day: TradingDay, portfolioName: PortfolioName, stock: Stock, value: MonetaryValue)

trait PortfolioValueRepository {
  def updateValue(day: TradingDay, portfolioName: PortfolioName, stock: Stock, value: MonetaryValue)(implicit ec: ExecutionContext): Future[Unit] =
    update(List(PortfolioValuationRecord(day, portfolioName, stock, value)))

  def update(records: List[PortfolioValuationRecord])(implicit ec: ExecutionContext): Future[Unit]
}
