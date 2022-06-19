package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.models.{ClosingPrice, Stock}

object AccountCalculator {
  def valuate(
      account: Account,
      closingPricesInAllCurrencies: List[ClosingPrice]
  ): AccountValuation = {
    val valuesForCurrency = account.holdingsForCurrencies.map {
      case (currency, holdings) =>
        val forStocks = holdings.stocks.map { case (stock, shares) =>
          closingPricesInAllCurrencies.find(cp =>
            (cp.stock == stock) && (cp.currency == currency)
          ) match {
            case Some(price) =>
              (stock, shares * price.amount)
            case _ =>
              // this should never happen, we should have checked for missing prices already
              throw new Exception(
                s"AccountValuation is missing a price for '${stock.symbol}' in '${currency}'"
              )
          }
        }
        // TODO: just treat it like a stock?
        val forCash = Map(Stock("Cash") -> holdings.cash)

        (currency, forStocks ++ forCash)
    }

    AccountValuation(account.name, valuesForCurrency)
  }
}
