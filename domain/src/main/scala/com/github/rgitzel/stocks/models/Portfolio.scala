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
    portfolios.flatMap( portfolio => checkForMissingPrices(portfolio, closingPrices)).distinct
  }

  def checkForMissingPrices(portfolio: Portfolio, closingPrices: List[ClosingPrice]): List[MissingPriceError] = {
    val pricedStocksForCurrencies = closingPrices.map(_.currency).distinct.map{ currency =>
      (currency, closingPrices.filter(_.currency == currency).map(_.stock))
    }.toMap
    portfolio.stocksForCurrencies.toList.flatMap{ case (currency, portfolioStocks) =>
      val unpricedStocks = pricedStocksForCurrencies.get(currency) match {
        case None =>
          // we don't have _any_ prices in that currency!
          portfolioStocks
        case Some(pricedStocksForCurrency) =>
          portfolioStocks.diff(pricedStocksForCurrency)
      }
      unpricedStocks.map(MissingPriceError(_, currency))
    }
  }
}