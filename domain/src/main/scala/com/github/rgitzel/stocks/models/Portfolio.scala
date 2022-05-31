package com.github.rgitzel.stocks.models

case class Portfolio(name: PortfolioName, shareCountsForStocks: Map[Stock,Int])
