package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.models
import com.github.rgitzel.stocks.models.{Currency, PortfolioName, Stock, Transaction}

import scala.util.{Failure, Success, Try}

object QuickenTransactionParser {
  // e.g. "LIRA USD AAPL 2/01/16 Bought 6"
  def fromString(s: String): Try[(PortfolioName, Currency, Transaction)] =
    s.split(" ").toList match {
      case List(portfolio, currency, symbol, day, action, amount) =>
        (TradingDayParser.fromString(day), QuickenTransactionDetailsParser.fromStrings(action, amount)) match {
          case (Success(tradingDay), Success(details)) =>
            Success((
              PortfolioName(portfolio),
              Currency(currency),
              models.Transaction(
                tradingDay,
                Stock(symbol),
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
