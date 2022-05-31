package com.github.rgitzel.stocks.models

case class PortfolioValue(name: PortfolioName, currency: Currency, valuesForStocks: Map[Stock,Double]) {
  val totalValue = valuesForStocks.map(_._2).sum
}
