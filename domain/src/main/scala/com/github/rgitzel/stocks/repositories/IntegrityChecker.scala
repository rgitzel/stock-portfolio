package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.accounts.Account
import com.github.rgitzel.stocks.models.ClosingPrice
import com.github.rgitzel.stocks.models.errors.MissingPriceError

// TODO: this needs a better name
object IntegrityChecker {
  def checkForMissingPrices(accounts: List[Account], closingPrices: List[ClosingPrice]): List[MissingPriceError] = {
    val stocksWithPricesForAGivenCurrency = closingPrices.map(_.currency).distinct.map{ currency =>
      (currency, closingPrices.filter(_.currency == currency).map(_.stock))
    }.toMap
    val allErrors = accounts.flatMap{ account =>
      account.holdingsForCurrencies.toList.flatMap{ case (currency, holdings) =>
        val stocksHeldInThisCurrency = holdings.stocks.keys.toList
        val stocksWithoutPricesInThisCurrency = stocksWithPricesForAGivenCurrency.get(currency) match {
          case Some(stocksWithPricesInThisCurrency) =>
            stocksHeldInThisCurrency.diff(stocksWithPricesInThisCurrency)
          case None =>
            // we don't have _any_ prices in that currency!
            // TODO: is this an error?
            stocksHeldInThisCurrency
        }
        stocksWithoutPricesInThisCurrency.map(MissingPriceError(_, currency))
      }
    }
    allErrors.distinct
  }

}
