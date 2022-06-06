package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.models.errors.MissingPriceError
import com.github.rgitzel.stocks.money.{Currency, _}

case class Portfolio(name: PortfolioName, shareCountsForStocksForCurrencies: Map[Currency,Map[Stock,Int]]) {
  // which currencies?
  lazy val currencies: List[Currency] = shareCountsForStocksForCurrencies.keys.toList
  // which stocks do we have in a given currency?
  lazy val stocksForCurrencies: Map[Currency,List[Stock]] = shareCountsForStocksForCurrencies.view.mapValues(_.keys.toList).toMap
}

object Portfolio {
  def checkForMissingPrices(portfolios: List[Portfolio], closingPrices: List[ClosingPrice]): List[MissingPriceError] = {
    val stocksWithPricesForAGivenCurrency = closingPrices.map(_.currency).distinct.map{ currency =>
      (currency, closingPrices.filter(_.currency == currency).map(_.stock))
    }.toMap
    val allErrors = portfolios.flatMap{ portfolio =>
      portfolio.stocksForCurrencies.toList.flatMap{ case (currency, portfolioStocks) =>
        val stocksWithoutPricesInThisCurrency = stocksWithPricesForAGivenCurrency.get(currency) match {
          case Some(stocksWithPricesInThisCurrency) =>
            portfolioStocks.diff(stocksWithPricesInThisCurrency)
          case None =>
            // we don't have _any_ prices in that currency!
            // TODO: is this an error?
            portfolioStocks
        }
        stocksWithoutPricesInThisCurrency.map(MissingPriceError(_, currency))
      }
    }
    allErrors.distinct
  }

  def checkForMissingPrices(portfolio: Portfolio, closingPrices: List[ClosingPrice]): List[MissingPriceError] = {
    checkForMissingPrices(List(portfolio), closingPrices)
  }
}