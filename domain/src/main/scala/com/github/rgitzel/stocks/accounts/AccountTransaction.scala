package com.github.rgitzel.stocks.accounts

sealed trait AccountTransaction

final case class StockPurchased(shareCount: Int) extends AccountTransaction
final case class StockSold(shareCount: Int) extends AccountTransaction
final case class StockSplit(multiplier: Int) extends AccountTransaction

