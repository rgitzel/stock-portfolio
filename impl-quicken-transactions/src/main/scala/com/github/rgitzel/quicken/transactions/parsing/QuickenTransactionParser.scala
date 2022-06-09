package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.accounts.{AccountActivity, AccountName}
import com.github.rgitzel.stocks.models.Stock
import com.github.rgitzel.stocks.money.Currency

import scala.util.{Failure, Success, Try}

object QuickenTransactionParser {
  // e.g. "LIRA USD AAPL 2/01/16 Bought 6"
  def fromString(s: String): Try[(AccountName, AccountActivity)] = {
    s.trim match {
      case "" =>
        failure(s, "empty string")
      case trimmed =>
        trimmed.split(" ").toList match {
          case List(portfolio, currency, symbol, day, action, amount) =>
            (TradingDayParser.fromString(day), QuickenTransactionDetailsParser.fromStrings(action, amount)) match {
              case (Success(tradingDay), Success(details)) =>
                Success((
                  AccountName(portfolio),
                  AccountActivity(
                    tradingDay,
                    Stock(symbol),
                    Currency(currency),
                    details
                  )
                ))
              case (Failure(t), _) =>
                failure(s, t.getMessage)
              case (_, Failure(t)) =>
                failure(s, t.getMessage)
            }
          case _ =>
            failure(s, "incorrect number of values")
        }
    }
  }

  def failure(s: String, reason: String) =
    Failure(new IllegalArgumentException(s"invalid transaction '${s}' (${reason})"))
}
