package com.github.rgitzel.stocks.models

final case class PortfolioJournal(name: PortfolioName, transactions: List[Transaction]) {
  def portfolioAsOf(day: TradingDay): Portfolio = {
    val shareCountsForStocks = transactions
      .filter(_.tradingDay < day)
      .groupBy(_.currency)
      .view.mapValues { x =>
      x.groupBy(_.stock)
        .view.mapValues { transactions =>
        transactions.map(_.action).foldLeft(0) { case (numberOfShares, details) =>
          details match {
            case StockPurchased(shareCount) =>
              numberOfShares + shareCount
            case StockSold(shareCount) =>
              numberOfShares - shareCount
            case StockSplit(multiplier) =>
              numberOfShares * multiplier
          }
        }
      }
        .filter(_._2 > 0)
        .toMap
    }.toMap

    Portfolio(name, shareCountsForStocks)
  }
}
