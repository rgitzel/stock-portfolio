package com.github.rgitzel.stocks.models.errors

import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.Currency

case class MissingPriceError(stock: Stock, currency: Currency)
