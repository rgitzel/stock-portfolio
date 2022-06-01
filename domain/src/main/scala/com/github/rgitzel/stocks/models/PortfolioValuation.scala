package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.money.Currency

case class PortfolioValuation(name: PortfolioName, valuesForStocksForCurrency: Map[Currency,Map[Stock,Double]]) {
//  val totalValue = valuesForStocks.map(_._2).sum
}
