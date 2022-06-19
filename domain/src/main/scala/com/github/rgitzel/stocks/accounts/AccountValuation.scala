package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.models.Stock
import com.github.rgitzel.stocks.money._

case class AccountValuation(
    name: AccountName,
    valuesForStocksForCurrency: Map[Currency, Map[Stock, Double]]
)
