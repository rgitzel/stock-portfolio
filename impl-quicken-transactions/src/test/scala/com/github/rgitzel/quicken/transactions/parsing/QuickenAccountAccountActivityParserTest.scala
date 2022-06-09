package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.accounts.{StockPurchased, StockSold, StockSplit}
import org.scalatest.TryValues
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

class QuickenAccountAccountActivityParserTest extends AnyFlatSpecLike with TryValues {
  import QuickenTransactionDetailsParser._
  
  "apply" should "parse a buy correctly" in {
    fromStrings("Bought", "100").success.value should be (StockPurchased(100))
  }

  it should "parse a sale correctly" in {
    // note that the value is _negative_ in the Quicken file
    fromStrings("Sold", "-5").success.value should be (StockSold(5))
  }

  it should "parse a split correctly" in {
    fromStrings("StkSplit", "50:5").success.value should be (StockSplit(10))
  }

  it should "fail on unrecognized action" in {
    fromStrings("Foo", "123").failure.exception.getMessage should be ("unrecognized transaction type 'Foo'")
  }

  // bad values
  List(
    ("Bought", "f5", "bad purchase value"),
    ("Sold", "f5", "bad sale value"),
    ("StkSplit", "50:f5", "bad stock split value")
  ).foreach{ case (action, value, reason) =>
    it should s"fail on ${reason}" in {
      fromStrings(action, value).failure.exception.getMessage should be (failedValue(value).exception.getMessage)
    }
  }
}
