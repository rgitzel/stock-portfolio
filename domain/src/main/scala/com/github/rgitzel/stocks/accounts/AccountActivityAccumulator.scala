package com.github.rgitzel.stocks.accounts

import scala.util.Try
import scala.util.control.NonFatal

object AccountActivityAccumulator {
  def accumulatedShareCountAndCash(activities: List[AccountActivity]): AccountBalance =
    activities
      .sortBy(_.tradingDay)
      .map(_.action)
      .foldLeft(AccountBalance(0, 0.0)) { case (balanceSoFar, transaction) =>
        Try {
          val newBalance = updateBalance(balanceSoFar, transaction)
          transaction match {
//            case Deposit(_) | Dividend(_) | Withdrawal(_) =>
//              println(s"${balanceSoFar} -> ${newBalance} (${transaction})")
//              case _: StockPurchased =>
//                println(s"${balanceSoFar} -> ${newBalance} (${transaction})")
//            case _: StockSold =>
//              println(s"${balanceSoFar} -> ${newBalance} (${transaction})")
            case _ =>
          }
          newBalance
        }
          .recover{
            case NonFatal(t) =>
              println(s"ignoring failed transaction '$transaction': ${t.getMessage}")
              balanceSoFar
          }
          .get
      }

  private def updateBalance(balanceSoFar: AccountBalance, transaction: AccountTransaction) =
    transaction match {
      case Deposit(value) =>
        balanceSoFar
          .plusCash(value)
      case Dividend(value) =>
        balanceSoFar
          .plusCash(value)
      case StockPurchased(shareCount, price, commission) =>
        balanceSoFar
          .plusShares(shareCount)
          .minusCash(shareCount * price + commission)
      case StockSold(shareCount, price, commission) =>
        balanceSoFar
          .minusShares(shareCount)
          .plusCash(shareCount * price - commission)
      case StockSplit(multiplier) =>
        balanceSoFar
          .copy(numberOfShares = balanceSoFar.numberOfShares * multiplier)
      case Withdrawal(value) =>
        balanceSoFar
          .minusCash(value)
      case _ =>
        // typically see this only when adding a new type
        println(s"unrecognized transaction type '$transaction'")
        balanceSoFar
    }

}
