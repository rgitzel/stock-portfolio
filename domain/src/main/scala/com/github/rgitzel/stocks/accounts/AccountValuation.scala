package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.accounts
import com.github.rgitzel.stocks.models.{ClosingPrice, Stock}
import com.github.rgitzel.stocks.money._

case class AccountValuation(name: AccountName, valuesForStocksForCurrency: Map[Currency,Map[Stock,Double]])

object AccountValuation {
  // TODO: is this the right place?
  def apply(account: AccountHoldings, closingPricesInAllCurrencies: List[ClosingPrice]): AccountValuation = {
    val values = account.shareCountsForStocksForCurrencies.map { case (currency, holdings) =>
      val valuesForHoldings = holdings.map{ case (stock, shares) =>
        closingPricesInAllCurrencies.find(cp => (cp.stock == stock) && (cp.currency == currency)) match {
          case Some(price) =>
            (stock, shares * price.amount)
          case _ =>
            throw new Exception(s"AccountValuation is missing a price for '${stock.symbol}' in '${currency}'")
        }
      }
      (currency, valuesForHoldings)
    }
    accounts.AccountValuation(account.name, values)
  }
}