package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.models.{StockPurchased, StockSold, StockSplit, TransactionDetails}

import scala.util.{Failure, Success, Try}

object QuickenTransactionDetailsParser {
  // e.g. "Bought" "100"
  def fromStrings(action: String, value: String): Try[TransactionDetails] = {
    action match {
      case "Bought" =>
        asInt(value).map(StockPurchased)
      case "Sold" =>
        asInt(value).map(StockSold)
      case "StkSplit" =>
        value.split(":", 2).toList match {
          case List(s1, s2) =>
            (asInt(s1), asInt(s2)) match {
              case (Success(m), Success(n)) =>
                Success(StockSplit(m / n))
              case _ =>
                failedValue(value)
            }
          case _ =>
            failedValue(value)
        }
      case otherwise =>
        Failure(new IllegalArgumentException(s"unrecognized transaction type '${otherwise}'"))
    }
  }

  def asInt(value: String) =
    Try(value.toInt)
      .recoverWith { _ => failedValue(value) }

  def failedValue(value: String) = Failure(new IllegalArgumentException(s"invalid transaction value ${value}"))
}
