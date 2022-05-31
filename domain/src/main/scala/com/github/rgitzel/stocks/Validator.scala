package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.models._

class Validator(currency: Currency) {
  def allDataExistsForPortfolio(portfolio: Portfolio, prices: Map[Stock,MonetaryValue], exchangeRates: Map[ConversionCurrencies,Double]): Option[String] = {
    val expectedStocks = portfolio.shareCountsForStocks.keys.toSet
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
        Some(s"portfolio ${portfolio.name} missing stocks: '${missingStocks}' and/or missing conversions: '${missingExchangeRates}''")
    }
  }

  def problemsWithRequiredDataForPortfolios(portfolios: List[Portfolio], prices: Map[Stock,MonetaryValue], exchangeRates: Map[ConversionCurrencies,Double]): List[String] = {
    portfolios.foldLeft(List[String]()){ case (accumulatedErrors, portfolio) =>
      allDataExistsForPortfolio(portfolio, prices, exchangeRates) match {
        case Some(error) =>
          accumulatedErrors :+ error
        case _ =>
          accumulatedErrors
      }
    }
  }
}
