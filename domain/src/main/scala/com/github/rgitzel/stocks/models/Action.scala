package com.github.rgitzel.stocks.models

sealed trait Action

final case class StockPurchased(shareCount: Int) extends Action
final case class StockSold(shareCount: Int) extends Action
final case class StockSplit(multiplier: Int) extends Action

