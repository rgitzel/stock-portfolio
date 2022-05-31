package com.github.rgitzel.stocks.models

sealed trait TransactionDetails

final case class StockPurchased(shareCount: Int) extends TransactionDetails
final case class StockSold(shareCount: Int) extends TransactionDetails
final case class StockSplit(multiplier: Int) extends TransactionDetails

