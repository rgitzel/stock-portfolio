package com.github.rgitzel.stocks.models

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

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
    ensureGreater(td, TradingDay(4, 31, 2022))
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
}
