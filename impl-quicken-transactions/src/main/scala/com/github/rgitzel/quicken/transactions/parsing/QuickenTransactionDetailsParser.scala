package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.quicken.transactions.parsing.QuickenTransactionDetailsParser.{asDouble, asInt}
import com.github.rgitzel.stocks.accounts.{AccountTransaction, Deposit, Dividend, StockPurchased, StockSold, StockSplit, Withdrawal}

import scala.util.{Failure, Success, Try}

object QuickenTransactionDetailsParser {
  // e.g. "Bought 100"
  def fromStrings(action: String, argument: String): Try[AccountTransaction] = {
    val arguments = argument.split("\\|").toList
    action match {
      case "Added" =>
        asInt(argument) match {
          case Success(shares) =>
            Success(StockPurchased(shares, 0.0, 0.0))
          case _ =>
            failedValue(argument)
        }
      case "Removed" =>
        asInt(argument) match {
          case Success(shares) =>
            Success(StockSold(-shares, 0.0, 0.0))
          case _ =>
            failedValue(argument)
        }
      case "Bought" =>
        arguments match {
          case List(s1, s2, s3, s4, s5) =>
            // Quicken makes the count _negative_, but we want it positive
            asInt(s1) match {
              case Success(count) =>
                (asDouble(s2), asDoubleOrZero(s3), asDouble(s4), asDouble(s5)) match {
                  case (Success(price), Success(commission), Success(total), Success(adjustment)) =>
                    val actualPrice = (-(total + adjustment) - commission) / count
                    Success(StockPurchased(count, actualPrice, commission))
                  case (Success(price), Success(commission), Success(total), _) =>
                    val actualPrice = (-total - commission) / count
                    Success(StockPurchased(count, actualPrice, commission))
                  case (Success(price), _, _, _) =>
                    Success(StockPurchased(count, price, 0.0))
                  case _ =>
                    failedValue(argument)
                }
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
        arguments match {
          case List(s1, s2, s3, s4, s5) =>
            // Quicken makes the count _negative_, but we want it positive
            asInt(s1).map(_ * -1) match {
              case Success(count) =>
                (asDouble(s2), asDouble(s3), asDouble(s4), asDouble(s5)) match {
                  case (Success(price), Success(commission), Success(total), Success(adjustment)) =>
                    val actualPrice = (total + adjustment + commission) / count
                    //                if (actualPrice != price)
                    //                  println(s"price difference ${actualPrice} vs ${price}")
                    Success(StockSold(count, actualPrice, commission))
                  case (Success(price), Success(commission), Success(total), _) =>
                    val actualPrice = (total + commission) / count
                    //                if (actualPrice != price)
                    //                  println(s"price difference ${actualPrice} vs ${price}")
                    Success(StockSold(count, actualPrice, commission))
                  case (Success(price), _, _, _) =>
                    Success(StockSold(count, price, 0.0))
                  case (_, _, _, _) =>
                    Success(StockSold(count, 0.0, 0.0))
                }
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
      case "IntInc" | "MiscInc" | "XIn" =>
        asDouble(argument) match {
          case Success(value) =>
            Success(Deposit(value))
          case _ =>
            failedValue(argument)
        }
      case "Cash" | "MiscExp" | "XOut" =>
        asDoubleOrZero(argument) match {
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

  def asDoubleOrZero(value: String) =
    Try(value.toDouble)
      .recoverWith { _ => Success(0.0) }

  def failedValue(value: String) =
    Failure(new IllegalArgumentException(s"invalid transaction argument '${value}'"))
}
