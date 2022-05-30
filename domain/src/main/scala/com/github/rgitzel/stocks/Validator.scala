package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.models.{ConversionCurrencies, Currency, MonetaryValue, Stock}

class Validator(currency: Currency) {
  def allDataExistsForPortfolio(label: String, portfolio: Map[Stock,Int], prices: Map[Stock,MonetaryValue], exchangeRates: Map[ConversionCurrencies,Double]): Option[String] = {
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
        Some(s"portfolio ${label} missing stocks: '${missingStocks}' and/or missing conversions: '${missingExchangeRates}''")
    }
  }

  def allDataExistsForPortfolios(portfolios: Map[String, Map[Stock,Int]], prices: Map[Stock,MonetaryValue], exchangeRates: Map[ConversionCurrencies,Double]): Option[String] = {
    val errors = portfolios.foldLeft(List[String]()){ case (accumulatedErrors, (label, portfolio)) =>
      allDataExistsForPortfolio(label, portfolio, prices, exchangeRates) match {
        case Some(error) =>
          accumulatedErrors :+ error
        case _ =>
          accumulatedErrors
      }
    }
    errors match {
      case Nil => None
      case _ => Some(errors.mkString(", "))
    }
  }
}
