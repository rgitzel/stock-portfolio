package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models.{PortfolioValuation, TradingWeek}
import com.github.rgitzel.stocks.money.{Currency, MonetaryValue}

case class WeeklyRecords(
    week: TradingWeek,
    accountStocks: List[AccountSingleStockValuationRecord],
    accounts: List[AccountTotalValuationRecord],
    portfolio: PortfolioValuationRecord
)

object WeeklyRecords {
  def buildRecords(
      week: TradingWeek,
      portfolioValuation: PortfolioValuation,
      totalsCurrency: Currency
  ): WeeklyRecords = {
    val (valueOfStockPerAccountRecords, accountSubtotalsByCurrencyRecords) =
      stockAndSubtotalRecords(week, portfolioValuation)

    val portfolioRecord = PortfolioValuationRecord(
      week.friday,
      portfolioValuation.totalValueInDesiredCurrency
    )

    WeeklyRecords(
      week,
      valueOfStockPerAccountRecords,
      accountSubtotalsByCurrencyRecords,
      portfolioRecord
    )
  }

  private def stockAndSubtotalRecords(
      week: TradingWeek,
      portfolioValuation: PortfolioValuation
  ) = {
    val byAccounts = portfolioValuation.accounts.map { accountValuation =>
      val stocksRecords = accountValuation.valuesForStocksForCurrency.flatMap {
        case (currency, holdings) =>
          holdings.toList
            .sortBy(_._1.symbol)
            .map { case (stock, value) =>
              AccountSingleStockValuationRecord(
                week.friday,
                accountValuation.name,
                stock,
                MonetaryValue(value, currency)
              )
            }
      }
      val accountSubtotalByCurrencyRecords = stocksRecords
        .groupBy(_.value.currency)
        .map { case (currency, stockRecords) =>
          AccountTotalValuationRecord(
            week.friday,
            accountValuation.name,
            MonetaryValue(
              stockRecords.map(_.value.value).sum,
              currency
            )
          )
        }
      (stocksRecords, accountSubtotalByCurrencyRecords)
    }
    (byAccounts.flatMap(_._1), byAccounts.flatMap(_._2))
  }
}
