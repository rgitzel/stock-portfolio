package com.github.rgitzel.stocks.money

case class MonetaryValue(value: Double, currency: Currency) {
  def converted(toCurrency: Currency, factor: Double): MonetaryValue =
    MonetaryValue(value * factor, toCurrency)
}
