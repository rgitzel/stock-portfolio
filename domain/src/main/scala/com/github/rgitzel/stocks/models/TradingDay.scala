package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.models.TradingDay.ordering

import java.time.temporal.ChronoUnit
import java.time._

/*
 * note that `month` is 1-indexed
 */
case class TradingDay(month: Int, day: Int, year: Int) extends Ordered[TradingDay] {
  def plus(days: Int): TradingDay =
    TradingDay(TradingDay.toInstant(this).plus(days, ChronoUnit.DAYS))

  def minus(days: Int): TradingDay =
    plus(-days)

  override def compare(that: TradingDay): Int = ordering.compare(this, that)
}


object TradingDay {
  implicit val ordering: Ordering[TradingDay] = new Ordering[TradingDay] {
    override def compare(x: TradingDay, y: TradingDay): Int =
      (x.year - y.year) match {
        case 0 =>
          (x.month - y.month) match {
            case 0 =>
              x.day - y.day
            case c =>
              c
          }
        case c =>
          c
      }
  }

  def toInstant(tradingDay: TradingDay): Instant = {
    val monthPrefix = if(tradingDay.month < 10) "0" else ""
    val dayPrefix = if(tradingDay.day < 10) "0" else ""
    val iso8601 = s"${tradingDay.year}-${monthPrefix}${tradingDay.month}-${dayPrefix}${tradingDay.day}T00:00:00.000Z"
    Instant.parse(iso8601)
  }

  def apply(ts: Instant): TradingDay = {
    // ay carumba, do Java time classes need to be so... surprising?
    //  https://stackoverflow.com/a/27855743/107444
    val localDate = ts.atOffset(ZoneOffset.UTC).toLocalDate()
    TradingDay(
      YearMonth.from(localDate).getMonthValue,
      MonthDay.from(localDate).getDayOfMonth,
      Year.from(localDate).getValue
    )
  }
}
