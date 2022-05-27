package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.models.{ConversionCurrencies, Currency, Price, Stock}

class Validator(currency: Currency) {
  def areComplete(portfolio: Map[Stock,Int], prices: Map[Stock,Price], exchangeRates: Map[ConversionCurrencies,Double]): Option[String] = {
    val expectedStocks = portfolio.keys.toSet
    val givenStocks = prices.keys.toSet
    val missingStocks = expectedStocks
      .diff(givenStocks)
      .mkString(",")

    val expectedExchangeRates = prices
      .filter(_._2.currency != currency)
      .values.map(p => ConversionCurrencies(p.currency, currency)).toSet
    val givenExchangeRates = exchangeRates.keys.toSet
    val missingExchangeRates = expectedExchangeRates.diff(givenExchangeRates).mkString(",")

    (missingStocks, missingExchangeRates) match {
      case ("", "") =>
        None
      case _ =>
        Some(s"missing stocks: ${missingStocks}\nmissing conversions: ${missingExchangeRates}")
    }
  }
}
