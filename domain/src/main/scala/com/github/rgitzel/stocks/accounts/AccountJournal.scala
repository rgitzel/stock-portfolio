package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.models.TradingDay
import com.github.rgitzel.stocks.money.Currency

/*
 * all of the transactions made in this account
 */
final case class AccountJournal(name: AccountName, transactions: List[AccountActivity]) {
  val currencies: List[Currency] = transactions.map(_.currency).distinct

  def accountAsOf(day: TradingDay): AccountHoldings = {
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

    AccountHoldings(name, shareCountsForStocks)
  }
}
