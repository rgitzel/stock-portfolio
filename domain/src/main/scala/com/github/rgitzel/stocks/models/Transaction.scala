package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.money.Currency

final case class Transaction(tradingDay: TradingDay, stock: Stock, currency: Currency, action: Action)
