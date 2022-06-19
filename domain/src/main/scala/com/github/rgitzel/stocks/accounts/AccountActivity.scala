package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.Currency

/*
 * note that the same stock can exist in an account under multiple currencies
 */
final case class AccountActivity(
    tradingDay: TradingDay,
    stock: Stock,
    currency: Currency,
    action: AccountTransaction
)
