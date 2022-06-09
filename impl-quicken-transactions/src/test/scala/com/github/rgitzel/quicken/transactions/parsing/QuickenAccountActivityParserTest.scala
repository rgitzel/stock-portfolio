package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.accounts.{AccountActivity, AccountName, StockPurchased}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.Currency
import org.scalatest.TryValues
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

class QuickenAccountActivityParserTest extends AnyFlatSpecLike with TryValues {
  "fromString" should "parse a valid string correctly" in {
    val expected = (AccountName("LIRA"), AccountActivity(TradingDay(2,1,2016), Stock("AAPL"), Currency("USD"), StockPurchased(6)))
    QuickenTransactionParser.fromString("LIRA USD AAPL 2/01/16 Bought 6").success.value should be (expected)
  }

  List(
    ("empty", "", "empty string"),
    ("no spaces", "LIRAUSDAAPL2/01/16Bought6", "incorrect number of values"),
    ("too many values", "LIRA USD AAPL 2/01/16 Bought 6 Foo", "incorrect number of values"),
    ("bad day", "LIRA USD AAPL 2/01 Bought 6", "invalid date string '2/01'"),
    ("unrecognized action", "LIRA USD AAPL 2/01/16 Dropped 6", "unrecognized transaction type 'Dropped'"),
  ).foreach{ case (reason, s, exceptionMessage) =>
    it should s"fail on $reason" in {
      // weird... comparing the `Exception` to itself fails?
      val expected = QuickenTransactionParser.failure(s, exceptionMessage).exception.getMessage
      QuickenTransactionParser.fromString(s).failure.exception.getMessage should be (expected)
    }
  }
}
