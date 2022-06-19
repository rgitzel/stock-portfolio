package com.github.rgitzel.stocks.accounts

import com.github.rgitzel.stocks.models.{Stock, TradingDay}
import com.github.rgitzel.stocks.money.Currency
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

// TODO: add tests for multiple currencies
class AccountJournalSpec extends AnyFlatSpecLike {
  val name = AccountName("foo")

  val stockA = Stock("A")
  val stockB = Stock("B")
  val stockC = Stock("C")

  val currency = Currency("x")
  
  // ==== one stock, two mirror transactions ===================

  val journalWithOneStockOnlyTwoTransactions = AccountJournal(
    name,
    List(
      AccountActivity(TradingDay(9, 12, 2017), stockA, currency, StockPurchased(400, 1.0, 3.0)),
      AccountActivity(TradingDay(10, 13, 2017), stockA, currency, StockSold(400, 1.1, 2.0))
    )
  )

  "just one stock and two transactions" should s"be empty before any transactions" in {
    val expected = Account(name, Map())
    journalWithOneStockOnlyTwoTransactions.accountAsOf(TradingDay(9, 1, 2017)) should be (expected)
  }

  List(
    ("after buying", TradingDay(9, 13, 2017), AccountHoldings(-403.0, Map[Stock,Int](stockA -> 400))),
    ("after selling", TradingDay(11, 1, 2017), AccountHoldings(-403.0 + 438.0, Map[Stock,Int]()))
  )
    .foreach{ case (reason, day, expectedHoldings) =>
      it should s"be correct ${reason}" in {
        val expected = Account(name, Map(currency -> expectedHoldings))
        journalWithOneStockOnlyTwoTransactions.accountAsOf(day) should be (expected)
      }
    }

  // ==== one stock ===================

  val journalWithOneStock = AccountJournal(
    name,
    List(
      AccountActivity(TradingDay(9, 12, 2017), stockA, currency, StockPurchased(400, 1.0, 2.0)), // -$402
      AccountActivity(TradingDay(10, 13, 2017), stockA, currency, StockPurchased(100, 2.0, 3.0)),// -$203 -> -$605
      AccountActivity(TradingDay(3, 28, 2019), stockA, currency, StockSold(300, 3.0, 4.0)),      // $896 -> $291
      AccountActivity(TradingDay(12, 1, 2021), stockA, currency, StockPurchased(200, 1.0, 4.0)), // $204 -> $87
      AccountActivity(TradingDay(12, 15, 2021), stockA, currency, StockSplit(3)),
      AccountActivity(TradingDay(3, 29, 2022), stockA, currency, StockSold(300, 1.0, 5.0)),      // $295 -> $382
    )
  )

  "just one stock and currency" should s"be empty before any transactions" in {
    journalWithOneStock.accountAsOf(TradingDay(9, 1, 2017)) should be (Account(name, Map()))
  }

  List(
    ("after two purchases", TradingDay(11, 1, 2017), AccountHoldings(-605.0, Map(stockA -> 500))),
    ("after first sale", TradingDay(3, 29, 2019), AccountHoldings(291.0, Map(stockA -> 200))),
    ("day before next purchase", TradingDay(11, 30, 2021), AccountHoldings(291.0, Map(stockA -> 200))),
    ("day of next purchase", TradingDay(12, 1, 2021), AccountHoldings(87.0, Map(stockA -> 400))),
    ("day before split", TradingDay(12, 14, 2021), AccountHoldings(87.0, Map(stockA -> 400))),
    ("day of split", TradingDay(12, 15, 2021), AccountHoldings(87.0, Map(stockA -> 1200))),
    ("after selling some", TradingDay(3, 30, 2022), AccountHoldings(382.0, Map(stockA -> 900))),
  )
    .foreach{ case (reason, day, expectedHoldings) =>
      it should s"be correct ${reason}" in {
        journalWithOneStock.accountAsOf(day) should be (Account(name, Map(currency -> expectedHoldings)))
      }
    }

  // ==== multiple stocks ===========

  val journalWithMultipleStocks = AccountJournal(
    name,
    List(
      AccountActivity(TradingDay(9, 12, 2017), stockA, currency, StockPurchased(400, 1.0, 2.0)), // -$402
      AccountActivity(TradingDay(10, 13, 2017), stockB, currency, StockPurchased(100, 2.0, 3.0)),// -$203 -> -$605
      AccountActivity(TradingDay(3, 28, 2019), stockA, currency, StockSold(300, 3.0, 4.0)),      // $896 -> $291
      AccountActivity(TradingDay(12, 1, 2021), stockC, currency, StockPurchased(200, 1.0, 4.0)), // $204 -> $87
      AccountActivity(TradingDay(12, 15, 2021), stockB, currency, StockSplit(3)),
      AccountActivity(TradingDay(3, 29, 2022), stockB, currency, StockSold(300, 1.0, 5.0)),      // $295 -> $382
    )
  )

  "multiple stocks" should s"be empty before any transactions" in {
    journalWithMultipleStocks.accountAsOf( TradingDay(9, 1, 2017)) should be (Account(name, Map()))
  }

  List(
    ("after two purchases", TradingDay(11, 1, 2017), AccountHoldings(-605.0, Map(stockA -> 400, stockB -> 100))),
    ("after first sale", TradingDay(3, 29, 2019), AccountHoldings(291.0, Map(stockA -> 100, stockB -> 100))),
    ("day before next purchase", TradingDay(11, 30, 2021), AccountHoldings(291.0, Map(stockA -> 100, stockB -> 100))),
    ("day of next purchase", TradingDay(12, 1, 2021), AccountHoldings(87.0, Map(stockA -> 100, stockB -> 100, stockC -> 200))),
    ("day before split", TradingDay(12, 14, 2021), AccountHoldings(87.0, Map(stockA -> 100, stockB -> 100, stockC -> 200))),
    ("day of split", TradingDay(12, 15, 2021), AccountHoldings(87.0, Map(stockA -> 100, stockB -> 300, stockC -> 200))),
    ("day after split", TradingDay(12, 16, 2021), AccountHoldings(87.0, Map(stockA -> 100, stockB -> 300, stockC -> 200))),
    ("after selling some", TradingDay(3, 30, 2022), AccountHoldings(382.0, Map(stockA -> 100, stockC -> 200))),
  )
    .foreach{ case (reason, day, expectedHoldings) =>
      it should s"be correct ${reason}" in {
        journalWithMultipleStocks.accountAsOf(day) should be (Account(name, Map(currency -> expectedHoldings)))
      }
    }
}
