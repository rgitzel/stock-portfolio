package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.models.TradingDay
import org.scalatest.TryValues
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

class TradingDayParserTest extends AnyFlatSpecLike with TryValues {
  "fromString" should "parse a valid string correctly" in {
    TradingDayParser.fromString("2/11/21").success.value should be (TradingDay(2, 11, 2021))
  }

  List(
    ("non-integer", "2/x11/21"),
    ("no slashes", "2345345"),
    ("too few slashes", "5/31"),
    ("too many slashes", "5/31/22/4"),
  ).foreach{ case (reason, s) =>
    it should s"fail on $reason" in {
      // weird... comparing the `Exception` to itself fails?
      TradingDayParser.fromString(s).failure.exception.getMessage should be (TradingDayParser.failure(s).exception.getMessage)
    }
  }
}
