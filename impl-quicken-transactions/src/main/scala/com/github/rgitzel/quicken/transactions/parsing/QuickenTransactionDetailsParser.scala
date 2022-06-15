package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.accounts.{AccountTransaction, Deposit, Dividend, StockPurchased, StockSold, StockSplit, Withdrawal}

import scala.util.{Failure, Success, Try}

object QuickenTransactionDetailsParser {
  // e.g. "Bought 100"
  def fromStrings(action: String, argument: String): Try[AccountTransaction] = {
    action match {
      case "Bought" =>
        argument.split(" ", 3).toList match {
          case List(s1, s2, s3) =>
            (asInt(s1), asDouble(s2), asDouble(s3)) match {
              case (Success(count), Success(price), Success(commission)) =>
                Success(StockPurchased(count, price, commission))
              case _ =>
                failedValue(argument)
            }
          case List(s1, s2) =>
            (asInt(s1), asDouble(s2)) match {
              case (Success(count), Success(price)) =>
                Success(StockPurchased(count, price, 0.0))
              case _ =>
                failedValue(argument)
            }
          case _ =>
            failedValue(argument)
        }
      case "Div" | "CGShort" | "CGMid" | "CGLong" =>
        asDouble(argument) match {
          case Success(value) =>
            Success(Dividend(value))
          case _ =>
            failedValue(argument)
        }
      case "Sold" =>
        argument.split(" ", 3).toList match {
          case List(s1, s2, s3) =>
            (asInt(s1), asDouble(s2), asDouble(s3)) match {
              case (Success(count), Success(price), Success(commission)) =>
                // Quicken makes the value _negative_, but we want it positive
                Success(StockSold(-count, price, commission))
              case _ =>
                failedValue(argument)
            }
          case List(s1, s2) =>
            (asInt(s1), asDouble(s2)) match {
              case (Success(count), Success(price)) =>
                // Quicken makes the value _negative_, but we want it positive
                Success(StockSold(-count, price, 0.0))
              case _ =>
                failedValue(argument)
            }
          case _ =>
            failedValue(argument)
        }
      case "StkSplit" =>
        argument.split(":", 2).toList match {
          case List(s1, s2) =>
            (asInt(s1), asInt(s2)) match {
              case (Success(m), Success(n)) =>
                Success(StockSplit(m / n))
              case _ =>
                failedValue(argument)
            }
          case _ =>
            failedValue(argument)
        }
      case "XIn" =>
        asDouble(argument) match {
          case Success(value) =>
            Success(Deposit(value))
          case _ =>
            failedValue(argument)
        }
      case "XOut" =>
        asDouble(argument) match {
          case Success(value) =>
            // Quicken makes the value _negative_, but we want it positive
            Success(Withdrawal(-value))
          case _ =>
            failedValue(argument)
        }
      case otherwise =>
        Failure(new IllegalArgumentException(s"unrecognized transaction type '${otherwise}'"))
    }
  }

  def asInt(value: String) =
    Try(value.toInt)
      .recoverWith { _ => failedValue(value) }

  def asDouble(value: String) =
    Try(value.toDouble)
      .recoverWith { _ => failedValue(value) }

  def failedValue(value: String) =
    Failure(new IllegalArgumentException(s"invalid transaction argument '${value}'"))
}
