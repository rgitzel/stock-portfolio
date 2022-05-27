package com.github.rgitzel.stocks.models

/*
 * using `Tuple2[Currency,Currency]` (and the associated underscore
 *  variables) was becoming less readable...
 */
final case class ConversionCurrencies(from: Currency, to: Currency)
