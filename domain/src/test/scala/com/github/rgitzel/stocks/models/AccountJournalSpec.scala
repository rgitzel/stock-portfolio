package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.accounts
import com.github.rgitzel.stocks.accounts.{AccountHoldings, AccountActivity, AccountJournal, AccountName, StockPurchased, StockSold, StockSplit}
import com.github.rgitzel.stocks.money.Currency
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

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
      AccountActivity(TradingDay(9, 12, 2017), stockA, currency, StockPurchased(400)),
      accounts.AccountActivity(TradingDay(10, 13, 2017), stockA, currency, StockSold(400))
    )
  )

  "just one stock and two transactions" should s"be empty before any transactions" in {
    journalWithOneStock.accountAsOf( TradingDay(9, 1, 2017)) should be (AccountHoldings(name, Map()))
  }

  List(
    ("after selling", TradingDay(11, 1, 2017), Map[Stock,Int]())
  )
    .foreach{ case (reason, day, expectedCounts) =>
      it should s"be correct ${reason}" in {
        journalWithOneStockOnlyTwoTransactions.accountAsOf(day) should be (accounts.AccountHoldings(name, Map(currency -> expectedCounts)))
      }
    }

  // ==== one stock ===================

  val journalWithOneStock = AccountJournal(
    name,
    List(
      accounts.AccountActivity(TradingDay(9, 12, 2017), stockA, currency, StockPurchased(400)),
      accounts.AccountActivity(TradingDay(10, 13, 2017), stockA, currency, StockPurchased(100)),
      accounts.AccountActivity(TradingDay(3, 28, 2019), stockA, currency, StockSold(300)),
      accounts.AccountActivity(TradingDay(12, 1, 2021), stockA, currency, StockPurchased(200)),
      accounts.AccountActivity(TradingDay(12, 15, 2021), stockA, currency, StockSplit(3)),
      accounts.AccountActivity(TradingDay(3, 29, 2022), stockA, currency, StockSold(300)),
    )
  )

  "just one stock and currency" should s"be empty before any transactions" in {
    journalWithOneStock.accountAsOf( TradingDay(9, 1, 2017)) should be (AccountHoldings(name, Map()))
  }

  List(
    ("after two purchases", TradingDay(11, 1, 2017), Map(stockA -> 500)),
    ("after first sale", TradingDay(3, 29, 2019), Map(stockA -> 200)),
    ("day of next purchase", TradingDay(12, 1, 2021), Map(stockA -> 200)),
    ("day of split", TradingDay(12, 15, 2021), Map(stockA -> 400)),
    ("day after split", TradingDay(12, 16, 2021), Map(stockA -> 1200)),
    ("after selling some", TradingDay(3, 30, 2022), Map(stockA -> 900)),
  )
    .foreach{ case (reason, day, expectedCounts) =>
      it should s"be correct ${reason}" in {
        journalWithOneStock.accountAsOf(day) should be (accounts.AccountHoldings(name, Map(currency -> expectedCounts)))
      }
    }

  // ==== multiple stocks ===========

  val journalWithMultipleStocks = AccountJournal(
    name,
    List(
      accounts.AccountActivity(TradingDay(9, 12, 2017), stockA, currency, StockPurchased(400)),
      accounts.AccountActivity(TradingDay(10, 13, 2017), stockB, currency, StockPurchased(100)),
      accounts.AccountActivity(TradingDay(3, 28, 2019), stockA, currency, StockSold(300)),
      accounts.AccountActivity(TradingDay(12, 1, 2021), stockC, currency, StockPurchased(200)),
      accounts.AccountActivity(TradingDay(12, 15, 2021), stockB, currency, StockSplit(3)),
      accounts.AccountActivity(TradingDay(3, 29, 2022), stockB, currency, StockSold(300)),
    )
  )

  "multiple stocks" should s"be empty before any transactions" in {
    journalWithOneStock.accountAsOf( TradingDay(9, 1, 2017)) should be (AccountHoldings(name, Map()))
  }

  List(
    ("after two purchases", TradingDay(11, 1, 2017), Map(stockA -> 400, stockB -> 100)),
    ("after first sale", TradingDay(3, 29, 2019), Map(stockA -> 100, stockB -> 100)),
    ("day of next purchase", TradingDay(12, 1, 2021), Map(stockA -> 100, stockB -> 100)),
    ("day of split", TradingDay(12, 15, 2021), Map(stockA -> 100, stockB -> 100, stockC -> 200)),
    ("day after split", TradingDay(12, 16, 2021), Map(stockA -> 100, stockB -> 300, stockC -> 200)),
    ("after selling some", TradingDay(3, 30, 2022), Map(stockA -> 100, stockC -> 200)),
  )
    .foreach{ case (reason, day, expectedCounts) =>
      it should s"be correct ${reason}" in {
        journalWithMultipleStocks.accountAsOf(day) should be (accounts.AccountHoldings(name, Map(currency -> expectedCounts)))
      }
    }
}
