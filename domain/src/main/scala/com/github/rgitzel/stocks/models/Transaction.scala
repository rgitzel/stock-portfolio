package com.github.rgitzel.stocks.models

final case class Transaction(tradingDay: TradingDay, stock: Stock, details: TransactionDetails)
