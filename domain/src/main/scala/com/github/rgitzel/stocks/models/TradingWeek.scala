package com.github.rgitzel.stocks.models

case class TradingWeek(lastDay: TradingDay) {
  override def toString: String = s"week ending ${lastDay}"

  private val DAYS_IN_WEEK = 7

  def followingWeek: TradingWeek = TradingWeek(lastDay.plus(DAYS_IN_WEEK))
  def previousWeek: TradingWeek = TradingWeek(lastDay.minus(DAYS_IN_WEEK))

  // including this one
  def previousWeeks(n: Int): List[TradingWeek] =
    1.until(n).foldLeft(List(this)){ case (accumulated, _) => accumulated.head.previousWeek +: accumulated }
}
