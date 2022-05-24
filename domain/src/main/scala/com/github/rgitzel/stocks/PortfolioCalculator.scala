package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.repositories._

import scala.concurrent._

class PortfolioCalculator(forexRepository: ForexRepository, holdingsRepository: HoldingsRepository, pricesRepository: PricesRepository)(implicit ec: ExecutionContext) {
  def value(day: TradingDay, currency: Currency): Future[Map[Symbol,Double]] = {
    getStuff(day).map{ case (rates, prices, holdings) =>
      val converted = convertedPrices(prices, rates, currency)
      Map.empty
    }
  }

  private def getStuff(day: TradingDay) = {
    val rates = forexRepository.closingRates(day)
    val prices = pricesRepository.closingPrices(day)
    val holdings = holdingsRepository.holdings(day)
    for {
      rates <- rates
      prices <- prices
      holdings <- holdings
    }
    yield (rates, prices, holdings)
  }

  private def convertedPrices(prices: Map[Stock,Price], rates: Map[ConversionCurrencies,Double], convertTo: Currency) = {
    // since the `to` currency is fixed, we can simplify the rates Map
    val convertToRates = rates.view
      .filterKeys(_.to == convertTo)
      .map{ case (currencies, price) =>
        (currencies.from, price)
      }
      .toMap

    prices.map{ case(stock, originalPrice) =>
      val newPrice = if(originalPrice.currency == convertTo) {
        // already in the desired currency
        originalPrice
      } else {
        convertToRates.get(originalPrice.currency) match {
          case Some(rate) =>
            // do the conversion
            Price(originalPrice.value * rate, convertTo)
          case _ =>
            // still need to decide what to do here
            originalPrice
        }
      }
      (stock, newPrice)
    }
  }
}
