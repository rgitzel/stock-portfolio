package com.github.rgitzel.stocks.money

/*
 * using `Tuple2[Currency,Currency]` (and the associated underscore
 *  variables) was becoming less readable...
 */
final case class ConversionCurrencies(from: Currency, to: Currency) {
  lazy val reverse = ConversionCurrencies(to, from)
}
