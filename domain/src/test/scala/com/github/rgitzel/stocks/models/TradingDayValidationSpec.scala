package com.github.rgitzel.stocks.models

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

class TradingDayValidationSpec extends AnyFlatSpecLike {
  import TradingDayValidation._

  "ensureDayIsPossible" should "succeed on inputs corresponding to a real day" in {
    noException should be thrownBy ensureDayIsPossible(6, 9, 2022)
  }

  it should "succeed for February 29th in a leap year" in {
    noException should be thrownBy ensureDayIsPossible(2, 29, 2020)
  }

  List(
    (0, 9, 2022, "month too small"),
    (13, 9, 2022, "month too large"),
    (0, 9, 2022, "day too small"),
    (6, 32, 2022, "day too large"),
    (2, 30, 2022, "February 30th"),
    (2, 29, 2022, "February 29th in non-leap-year"),
  ).map{ case (month, day, year, reason) =>
    it should s"fail on ${reason}" in {
      an[IllegalArgumentException] should be thrownBy ensureDayIsPossible(month, day, year)
    }
  }


  "ensureIsWeekday" should "succeed on a Monday" in {
    noException should be thrownBy ensureIsWeekday(6, 6, 2022)
  }

  List(
    (6, 7, 2022, "Tuesday"),
    (6, 8, 2022, "Wednesday"),
    (6, 9, 2022, "Thursday"),
    (6, 10, 2022, "Friday"),
  ).foreach{ case (month, day, year, label) =>
    it should s"succeed on a ${label}" in {
      noException should be thrownBy ensureIsWeekday(month, day, year)
    }
  }

  List(
    (6, 5, 2022, "Sunday"),
    (6, 11, 2022, "Saturday"),
  ).foreach{ case (month, day, year, reason) =>
    it should s"fail on ${reason}" in {
      an[IllegalArgumentException] should be thrownBy ensureIsWeekday(month, day, year)
    }
  }
}
