package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.money.CashUtils

final case class AccountBalance(numberOfShares: Int, cash: Double) {
  val asTuple = (numberOfShares, cash)

  def plusCash(value: Double): AccountBalance = this.copy(cash = CashUtils.roundedToCents(this.cash + CashUtils.roundedToCents(value)))
  def minusCash(value: Double): AccountBalance = this.plusCash(-value)

  def minusShares(amount: Int): AccountBalance = this.copy(numberOfShares = this.numberOfShares - amount)
  def plusShares(amount: Int): AccountBalance = this.minusShares(-amount)
}
