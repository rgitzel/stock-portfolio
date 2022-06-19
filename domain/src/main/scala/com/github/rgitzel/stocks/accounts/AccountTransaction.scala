package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.money.CashUtils

sealed trait AccountTransaction {
  // this is for logging mostly
  val value: Double
}

final case class Deposit(value: Double) extends AccountTransaction

final case class Dividend(value: Double) extends AccountTransaction

// TODO: do we actually care about 'price'? if we just have 'total' we can avoid all that price rounding weirdness
final case class StockPurchased(
    shareCount: Int,
    price: Double,
    commission: Double
) extends AccountTransaction {
  val value = CashUtils.roundedToCents(-(shareCount * price) - commission)
}

final case class StockSold(shareCount: Int, price: Double, commission: Double)
    extends AccountTransaction {
  val value = CashUtils.roundedToCents((shareCount * price) - commission)
}

final case class StockSplit(multiplier: Int) extends AccountTransaction {
  val value = 0.0
}

final case class Withdrawal(value: Double) extends AccountTransaction
