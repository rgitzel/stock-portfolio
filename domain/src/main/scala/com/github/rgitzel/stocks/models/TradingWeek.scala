package com.github.rgitzel.stocks.models

import java.time.Instant

/*
 * over the long term, we don't care about individual days,
 *  it's more than enough to look at a portfolio from week to week
 */
case class TradingWeek(friday: TradingDay) {
  if (!friday.isFriday)
    throw new IllegalArgumentException(
      s"trading week must end in a Friday (not ${friday.dayOfWeek})"
    )

  override def toString: String = s"the week ending ${friday}"

  private val DAYS_IN_WEEK = 7

  def followingWeek: TradingWeek = TradingWeek(friday.plus(DAYS_IN_WEEK))
  def previousWeek: TradingWeek = TradingWeek(friday.minus(DAYS_IN_WEEK))

  // inclusive
  def previousWeeks(n: Int): List[TradingWeek] =
    1.until(n).foldLeft(List(this)) { case (accumulated, _) =>
      accumulated.head.previousWeek +: accumulated
    }

  // also inclusive
  def to(endingWeek: TradingWeek): List[TradingWeek] = {
    def r(
        accumulated: List[TradingWeek],
        candidate: TradingWeek
    ): List[TradingWeek] = {
      if (candidate.friday > endingWeek.friday)
        accumulated
      else
        r(accumulated :+ candidate, candidate.followingWeek)
    }
    r(List(this), this.followingWeek)
  }
}

object TradingWeek {
  def apply(month: Int, day: Int, year: Int): TradingWeek =
    TradingWeek(TradingDay(month, day, year))

  def mostRecent(): TradingWeek = mostRecent(Instant.now())

  def mostRecent(relativeTo: Instant): TradingWeek =
    TradingWeek(TradingDay.previousFridayIncludingThisDay(relativeTo))

  def yearEnd(year: Int) = {
    val lastDay = TradingDay.lastOfYear(year)
    TradingWeek(TradingDay.previousFridayIncludingThisDay(lastDay))
  }
}
