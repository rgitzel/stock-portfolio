package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models.TradingWeek
import com.github.rgitzel.stocks.money.{Currency, MonetaryValue}

case class WeeklyRecords(
                          week: TradingWeek,
                          accountStocks: List[AccountStockValuationRecord],
                          accounts: List[AccountValuationRecord],
                          portfolio: PortfolioValuationRecord
                        )

object WeeklyRecords {
  def empty(week: TradingWeek):WeeklyRecords =
    WeeklyRecords(
      week,
      List[AccountStockValuationRecord](),
      List[AccountValuationRecord](),
      PortfolioValuationRecord(week.lastDay, MonetaryValue(0.0, Currency(""))) // TODO: awkward!
    )
}