package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.models.{ConversionCurrencies, Currency, Price, Stock}

/*
 * this assumes all values are present, so anything missing is an _exception_
 */
class Summarizer(currency: Currency) {
  def summarize(portfolio: Map[Stock, Int], prices: Map[Stock, Price], exchangeRates: Map[ConversionCurrencies, Double]) = {
    // normalize the prices
    val normalizedPrices = prices
      .view.mapValues{ originalPrice =>
        if (originalPrice.currency == currency) {
          originalPrice
        }
        else {
          val multiplier = exchangeRates.getOrElse(ConversionCurrencies(originalPrice.currency, currency), throw new Exception("no!"))
          Price(originalPrice.value * multiplier, currency)
        }
    }

    portfolio.map { case (stock, shares) =>
      normalizedPrices.get(stock) match {
        case Some(price) =>
          (stock, shares * price.value)
        case _ =>
          throw new Exception("no!")
      }
    }
  }
}
