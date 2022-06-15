package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.models.TradingDay.ordering
import com.github.rgitzel.stocks.models.TradingDayValidation.toIsoTimestampString

import java.time._
import java.time.format.{DateTimeFormatter, ResolverStyle}
import java.time.temporal.ChronoUnit
import scala.util.{Success, Try}
import scala.util.control.NonFatal

/*
 * for the purposes of stock prices, we just care about the "day" (in particular
 *  the end of the day) and not the particular time
 *
 * notes:
 *  - `month` is 1-indexed
 */
case class TradingDay(month: Int, day: Int, year: Int) extends Ordered[TradingDay] {
  TradingDayValidation.ensureDayIsPossible(month, day, year)
  TradingDayValidation.ensureIsWeekday(month, day, year)

  val isFriday: Boolean = TradingDayValidation.isFriday(month, day, year)

  val dayOfWeek: String = TradingDay.dayOfWeek(month, day, year).toString

  override def toString: String = s"${month}/${day}/${year}"

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

  def toInstant(tradingDay: TradingDay): Instant =
    Instant.parse(TradingDayValidation.toIsoTimestampString(tradingDay.month, tradingDay.day, tradingDay.year))

  def apply(ts: Instant): TradingDay = {
    // ay carumba, do Java time classes need to be so... unexpected?
    //  https://stackoverflow.com/a/27855743/107444
    val localDate = ts.atOffset(ZoneOffset.UTC).toLocalDate()
    TradingDay(
      YearMonth.from(localDate).getMonthValue,
      MonthDay.from(localDate).getDayOfMonth,
      Year.from(localDate).getValue
    )
  }

  def previousFriday(relativeTo: Instant): TradingDay = {
    def r(candidate: Instant): TradingDay = {
      Try(TradingDay(candidate)) match {
        case Success(td) if td.isFriday =>
          td
        case _ =>
          r(candidate.minus(1, ChronoUnit.DAYS))
      }
    }
    r(relativeTo)
  }

  def dayOfWeek(month: Int, day: Int, year: Int): DayOfWeek =
  // TODO: standardize on `US/Eastern`
    Instant.parse(toIsoTimestampString(month, day, year)).atZone(ZoneId.of("UTC")).getDayOfWeek()
}

object TradingDayValidation {
  def toIsoDayString(month: Int, day: Int, year: Int): String = {
    val monthPrefix = if(month < 10) "0" else ""
    val dayPrefix = if(day < 10) "0" else ""
    s"${year}-${monthPrefix}${month}-${dayPrefix}${day}"
  }

  def toIsoTimestampString(month: Int, day: Int, year: Int): String = {
    val isoDay = toIsoDayString(month, day, year)
    val isoTime = "00:00:00.000Z"
    s"${isoDay}T${isoTime}"
  }

  // https://stackoverflow.com/a/39649815/107444
  private val f = DateTimeFormatter.ofPattern ( "uuuu-MM-dd" ).withResolverStyle ( ResolverStyle.STRICT )

  def ensureDayIsPossible(month: Int, day: Int, year: Int): Unit = {
    val s = toIsoDayString(month, day, year)
    try {
      f.parse(s)
      ()
    }
    catch { case NonFatal(_) =>
        throw new IllegalArgumentException(s"invalid day '${day}")
    }
  }

  // https://stackoverflow.com/a/56366534/107444
  def ensureIsWeekday(month: Int, day: Int, year: Int): Unit = {
    val d = TradingDay.dayOfWeek(month, day, year)
    if (d.getValue() > 5)
      throw new IllegalArgumentException(s"'${toIsoDayString(month, day, year)}' is a ${d}")
  }

  def isFriday(month: Int, day: Int, year: Int): Boolean =
    TradingDay.dayOfWeek(month, day, year) == DayOfWeek.FRIDAY
}