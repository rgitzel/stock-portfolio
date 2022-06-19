package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models.{PortfolioValuation, Stock, TradingWeek}
import com.github.rgitzel.stocks.money.{Currency, MonetaryValue}

case class WeeklyRecords(
    week: TradingWeek,
    accountStocks: List[AccountStockValuationRecord],
    accounts: List[AccountValuationRecord],
    portfolio: PortfolioValuationRecord
)

object WeeklyRecords {
  def buildRecords(
      week: TradingWeek,
      portfolioValuation: PortfolioValuation,
      totalsCurrency: Currency
  ): WeeklyRecords = {
    val records = stockAndSubtotalRecords(week, portfolioValuation)

    val total = records
      .map(_._2.map(_.value.value).sum)
      .sum

    val totalsRecord = PortfolioValuationRecord(
      week.friday,
      MonetaryValue(
        total,
        totalsCurrency
      )
    )

    WeeklyRecords(
      week,
      records.flatMap(_._1.toList),
      records.flatMap(_._2.toList),
      totalsRecord
    )
  }

  private def stockAndSubtotalRecords(
      week: TradingWeek,
      portfolioValuation: PortfolioValuation
  ) =
    portfolioValuation.accounts.map { accountValuation =>
      val stocksRecords = accountValuation.valuesForStocksForCurrency.flatMap {
        case (currency, holdings) =>
          holdings.toList
            .sortBy(_._1.symbol)
            .map { case (stock, value) =>
              AccountStockValuationRecord(
                week.friday,
                accountValuation.name,
                stock,
                MonetaryValue(value, currency)
              )
            }
      }
      val subtotalsRecords = stocksRecords
        .groupBy(_.value.currency)
        .map { case (currency, stockRecords) =>
          AccountValuationRecord(
            week.friday,
            accountValuation.name,
            MonetaryValue(
              stockRecords.map(_.value.value).sum,
              currency
            )
          )
        }
      (stocksRecords, subtotalsRecords)
    }
}
