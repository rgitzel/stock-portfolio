package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.money._

case class PortfolioValuation(name: PortfolioName, valuesForStocksForCurrency: Map[Currency,Map[Stock,Double]]) {
//  val totalValue = valuesForStocks.map(_._2).sum
}

object PortfolioValuation {
  def apply(portfolio: Portfolio, closingPricesInAllCurrencies: List[ClosingPrice]): PortfolioValuation = {
    val values = portfolio.shareCountsForStocksForCurrencies.map { case (currency, holdings) =>
      val valuesForHoldings = holdings.map{ case (stock, shares) =>
        closingPricesInAllCurrencies.find(cp => (cp.stock == stock) && (cp.currency == currency)) match {
          case Some(price) =>
            (stock, shares * price.amount)
          case _ =>
            throw new Exception(s"valuate is missing a price for '${stock.symbol}' in '${currency}'")
        }
      }
      (currency, valuesForHoldings)
    }
    PortfolioValuation(portfolio.name, values)
  }
}