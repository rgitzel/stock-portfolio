package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.models._

/*
 * this assumes all values are present, so anything missing is an _exception_
 */
class PortfolioValuator(currency: Currency) {
  def valuate(portfolio: Portfolio, prices: Map[Stock, MonetaryValue], exchangeRates: Map[ConversionCurrencies, Double]): PortfolioValue = {
    // normalize the prices
    val normalizedPrices = prices
      .view.mapValues{ originalPrice =>
        if (originalPrice.currency == currency) {
          originalPrice
        }
        else {
          val multiplier = exchangeRates.getOrElse(ConversionCurrencies(originalPrice.currency, currency), throw new Exception("no!"))
          MonetaryValue(originalPrice.value * multiplier, currency)
        }
    }

    val values = portfolio.shareCountsForStocks.map { case (stock, shares) =>
      normalizedPrices.get(stock) match {
        case Some(price) =>
          (stock, shares * price.value)
        case _ =>
          throw new Exception("no!")
      }
    }

    PortfolioValue(portfolio.name, currency, values)
  }
}
