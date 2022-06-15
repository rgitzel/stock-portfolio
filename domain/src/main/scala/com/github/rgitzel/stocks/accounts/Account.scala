package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.models.Stock
import com.github.rgitzel.stocks.money.Currency

case class Account(name: AccountName, holdingsForCurrencies: Map[Currency,AccountHoldings])

case class AccountHoldings(cash: Double, stocks: Map[Stock,Int])