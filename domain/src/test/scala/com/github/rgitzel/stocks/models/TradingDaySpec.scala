package com.github.rgitzel.stocks.models

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

import java.time.Instant
import java.time.temporal.ChronoUnit

class TradingDaySpec extends AnyFlatSpecLike {
  import TradingDay.ordering

  val td = TradingDay(5, 31, 2022)

  "ordering" should "return 0 if equal" in {
    ordering.compare(td, td) should be (0)
  }

  it should "compare different days correctly" in {
    ensureGreater(td, TradingDay(5, 3, 2022))
  }

  it should "compare different months correctly" in {
    ensureGreater(td, TradingDay(1, 31, 2022))
  }

  it should "compare different years correctly" in {
    ensureGreater(td, TradingDay(5, 31, 2021))
  }

  private def ensureGreater(td1: TradingDay, td2: TradingDay) = {
    ordering.compare(td1, td2) should be > (0)
    ordering.compare(td2, td1) should be < (0)
  }

  val td1 = TradingDay(2, 1, 2016)
  val td2 = TradingDay(12, 15, 2020)
  val td3 = TradingDay(12, 14, 2020)
  val td4 = TradingDay(8, 31, 2021)
  val sortedDays = List(td1, td3, td2, td4)

  it should "sort sorted list correctly" in {
    sortedDays.sorted should be (sortedDays)
  }

  it should "sort reversed list correctly" in {
    sortedDays.reverse.sorted should be (sortedDays)
  }

  it should "sort mixed list correctly" in {
    List(td3, td1, td4, td2).sorted should be (sortedDays)
  }

  "constructor" should "succeed on inputs corresponding to a real day" in {
    noException should be thrownBy TradingDay(6, 9, 2022)
  }

  it should "succeed for February 29th in a leap year" in {
    noException should be thrownBy TradingDay(2, 29, 2016)
  }

  List(
    (2, 30, 2022, "February 30th"),
    (2, 29, 2022, "February 29th in non-leap-year"),
  ).map{ case (month, day, year, reason) =>
    it should s"fail on ${reason}" in {
      an[IllegalArgumentException] should be thrownBy TradingDay(month, day, year)
    }
  }

  val previousFriday = TradingDay(6, 3, 2022)
  val saturday = Instant.ofEpochSecond(1654370807L) // Saturday, June 4, 2022 7:26:47 PM

  "lastFriday" should "return the Friday before a Saturday" in {
    TradingDay.previousFriday(saturday) should be (previousFriday)
  }

  List(
    (saturday.plus(1, ChronoUnit.DAYS), "Sunday"),
    (saturday.plus(2, ChronoUnit.DAYS), "Monday"),
    (saturday.plus(3, ChronoUnit.DAYS), "Tuesday"),
    (saturday.plus(4, ChronoUnit.DAYS), "Wednesday"),
    (saturday.plus(5, ChronoUnit.DAYS), "Thursday")
  ).foreach{ case (ts, label) =>
    it should s"return the Friday before a ${label}" in {
      TradingDay.previousFriday(ts) should be (previousFriday)
    }
  }

  // TODO: should worry about the exact time
  it should s"return the same day on a Friday" in {
    TradingDay.previousFriday(saturday.plus(6, ChronoUnit.DAYS)) should be (previousFriday.plus(7))
  }
}
