package com.github.rgitzel.stocks.accounts

case class AccountName(s: String) {
  override def toString: String = s
}

object AccountName {
  implicit val ordering: Ordering[AccountName] = new Ordering[AccountName] {
    override def compare(x: AccountName, y: AccountName): Int = x.s.compare(y.s)
  }
}