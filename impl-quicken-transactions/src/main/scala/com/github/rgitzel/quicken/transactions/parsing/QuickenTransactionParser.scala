package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.models
import com.github.rgitzel.stocks.models.{PortfolioName, Stock, Transaction}
import com.github.rgitzel.stocks.money.Currency

import scala.util.{Failure, Success, Try}

object QuickenTransactionParser {
  // e.g. "LIRA USD AAPL 2/01/16 Bought 6"
  def fromString(s: String): Try[(PortfolioName, Transaction)] =
    s.split(" ").toList match {
      case List(portfolio, currency, symbol, day, action, amount) =>
        (TradingDayParser.fromString(day), QuickenTransactionDetailsParser.fromStrings(action, amount)) match {
          case (Success(tradingDay), Success(details)) =>
            // TODO: need better way to handle having Apple in two currencies
            val appleKludgeSymbol = if((symbol == "AAPL") & (currency == "CAD")) "AAPLCAD" else symbol
            Success((
              PortfolioName(portfolio),
              models.Transaction(
                tradingDay,
                Stock(appleKludgeSymbol),
                Currency(currency),
                details
              )
            ))
          case _ =>
            failure(s)
        }
      case _ =>
        failure(s)
    }

  def failure(s: String) = Failure(new IllegalArgumentException(s"invalid transaction '${s}''"))
}
