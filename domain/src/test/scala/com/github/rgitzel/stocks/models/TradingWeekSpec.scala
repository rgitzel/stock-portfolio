package com.github.rgitzel.stocks.models

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

class TradingWeekSpec extends AnyFlatSpecLike {
  val w = TradingWeek(6, 10, 2022)

  "to" should "return just the one" in {
    w.to(w) should be (List(w))
  }

  it should "return three weeks" in {
    val w2 = w.followingWeek
    val w3 = w2.followingWeek
    w.to(w3) should be (List(w, w2, w3))
  }

  List(
    (2017, TradingWeek(12, 29, 2017)),
    (2018, TradingWeek(12, 28, 2018)),
    (2019, TradingWeek(12, 27, 2019))
  )
    .foreach{ case (year, expectedWeek) =>
      "yearEnd" should s"return the right week for ${year}" in {
        TradingWeek.yearEnd(year) should be (expectedWeek)
      }
    }
}
