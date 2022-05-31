package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.models._
import org.scalatest.TryValues
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

class QuickenTransactionParserTest extends AnyFlatSpecLike with TryValues {
  "fromString" should "parse a valid string correctly" in {
    val expected = (PortfolioName("LIRA"), Currency("USD"), Transaction(TradingDay(2,1,2016), Stock("AAPL"), StockPurchased(6)))
    QuickenTransactionParser.fromString("LIRA USD AAPL 2/01/16 Bought 6").success.value should be (expected)
  }

  List(
    ("empty", ""),
    ("no spaces", "LIRAUSDAAPL2/01/16Bought6"),
    ("bad day", "LIRA USD AAPL 2/01 Bought 6"),
    ("unrecognized action", "LIRA USD AAPL 2/01/16 Dropped 6"),
  ).foreach{ case (reason, s) =>
    it should s"fail on $reason" in {
      // weird... comparing the `Exception` to itself fails?
      QuickenTransactionParser.fromString(s).failure.exception.getMessage should be (QuickenTransactionParser.failure(s).exception.getMessage)
    }
  }
}
