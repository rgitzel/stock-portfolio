package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.models.TradingDay
import com.github.rgitzel.stocks.money.{CashUtils, Currency}

/*
 * all of the transactions made in this account
 */
final case class AccountJournal(name: AccountName, activities: List[AccountActivity]) {
  val currencies: List[Currency] = activities.map(_.currency).distinct

  def accountAsOf(day: TradingDay): Account = {
    Account(
      name,
      activities
        .filter(_.tradingDay <= day)
        .groupBy(_.currency).view
        .map { case (currency, activities) =>
          val countsAndCashForStocks = activities.groupBy(_.stock)
            .view.map { case (stock, activitiesForStock) =>
              val x = AccountActivityAccumulator.accumulatedShareCountAndCash(activitiesForStock)
//              println(s"${currency} ${stock}\n  ${activitiesForStock.mkString("\n  ")}\n  ${x}")
              (stock, x)
            }.toMap
          val cash = CashUtils.roundedToCents(countsAndCashForStocks.values.map(_.cash).sum)
          val stocks = countsAndCashForStocks.mapValues(_.numberOfShares).toMap.filter(_._2 > 0)
          (currency, AccountHoldings(cash, stocks))
        }.toMap
    )
  }
}
