package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.models.errors.MissingPriceError
import com.github.rgitzel.stocks.money.{Currency, MonetaryValue}

case class Portfolio(name: PortfolioName, shareCountsForStocksForCurrencies: Map[Currency,Map[Stock,Int]]) {
  val stocksForCurrencies: Map[Currency,List[Stock]] = shareCountsForStocksForCurrencies.view.mapValues(_.keys.toList).toMap
}

object Portfolio {
  def checkForMissingPrices(portfolios: List[Portfolio], prices: Map[Stock, MonetaryValue]): List[MissingPriceError] = {
    portfolios.flatMap( portfolio => checkForMissingPrices(portfolio, prices)).distinct
  }

  def checkForMissingPrices(portfolio: Portfolio, prices: Map[Stock, MonetaryValue]): List[MissingPriceError] = {
    // extract the stocks we have prices for in each of the given currencies
    val priceCurrencies = prices.values.map(_.currency).toList.distinct
    val pricedStocksForCurrencies = priceCurrencies.map{ currency =>
      (currency, prices.filter(_._2.currency == currency).keys.toList)
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

  def valuate(portfolio: Portfolio, prices: Map[Stock, MonetaryValue]): PortfolioValuation = {
    val values = portfolio.shareCountsForStocksForCurrencies.map { case (currency, holdings) =>
      val valuesForHoldings = holdings.map{ case (stock, shares) =>
        prices.get(stock) match {
          case Some(price) =>
            (stock, shares * price.value)
          case _ =>
            throw new Exception(s"valuate is missing a price for '${stock.symbol}' in '${currency}'")
        }
      }
      (currency, valuesForHoldings)
    }

    PortfolioValuation(portfolio.name, values)
  }
}