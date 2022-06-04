package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.money.{Currency, MonetaryValue}

/*
 * an alternate way to represent how much a share of a given stock is worth
 */

final case class ClosingPrice(stock: Stock, currency: Currency, amount: Double) {
  val monetaryValue: MonetaryValue = MonetaryValue(amount, currency)
}

object ClosingPrice {
  def apply(stock: Stock, monetaryValue: MonetaryValue): ClosingPrice = ClosingPrice(stock, monetaryValue.currency, monetaryValue.value)
}
