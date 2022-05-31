package com.github.rgitzel.stocks.models

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

class PortfolioJournalSpec extends AnyFlatSpecLike {
  val name = PortfolioName("foo")

  val stockA = Stock("A")
  val stockB = Stock("B")
  val stockC = Stock("C")

  // ==== one stock, two mirror transactions ===================

  val journalWithOneStockOnlyTwoTransactions = PortfolioJournal(
    name,
    List(
      Transaction(TradingDay(9, 12, 2017), stockA, StockPurchased(400)),
      Transaction(TradingDay(10, 13, 2017), stockA, StockSold(400))
    )
  )

  List(
    ("before buying", TradingDay(9, 1, 2017), Map[Stock,Int]()),
    ("after selling", TradingDay(11, 1, 2017), Map[Stock,Int]())
  )
    .foreach{ case (reason, day, expectedCounts) =>
      it should s"be correct ${reason} (just one stock and two transactions)" in {
        journalWithOneStockOnlyTwoTransactions.portfolioAsOf(day) should be (Portfolio(name, expectedCounts))
      }
    }

  // ==== one stock ===================

  val journalWithOneStock = PortfolioJournal(
    name,
    List(
      Transaction(TradingDay(9, 12, 2017), stockA, StockPurchased(400)),
      Transaction(TradingDay(10, 13, 2017), stockA, StockPurchased(100)),
      Transaction(TradingDay(3, 29, 2019), stockA, StockSold(300)),
      Transaction(TradingDay(12, 1, 2021), stockA, StockPurchased(200)),
      Transaction(TradingDay(12, 15, 2021), stockA, StockSplit(3)),
      Transaction(TradingDay(3, 29, 2022), stockA, StockSold(300)),
    )
  )

  List(
    ("before started", TradingDay(9, 1, 2017), Map[Stock,Int]()),
    ("after two purchases", TradingDay(11, 1, 2017), Map(stockA -> 500)),
    ("after first sale", TradingDay(3, 30, 2019), Map(stockA -> 200)),
    ("day of next purchase", TradingDay(12, 1, 2021), Map(stockA -> 200)),
    ("day of split", TradingDay(12, 15, 2021), Map(stockA -> 400)),
    ("day after split", TradingDay(12, 16, 2021), Map(stockA -> 1200)),
    ("after selling some", TradingDay(3, 30, 2022), Map(stockA -> 900)),
  )
    .foreach{ case (reason, day, expectedCounts) =>
      it should s"be correct ${reason} (just one stock)" in {
        journalWithOneStock.portfolioAsOf(day) should be (Portfolio(name, expectedCounts))
      }
    }

  // ==== multiple stocks ===========

  val journalWithMultipleStocks = PortfolioJournal(
    name,
    List(
      Transaction(TradingDay(9, 12, 2017), stockA, StockPurchased(400)),
      Transaction(TradingDay(10, 13, 2017), stockB, StockPurchased(100)),
      Transaction(TradingDay(3, 29, 2019), stockA, StockSold(300)),
      Transaction(TradingDay(12, 1, 2021), stockC, StockPurchased(200)),
      Transaction(TradingDay(12, 15, 2021), stockB, StockSplit(3)),
      Transaction(TradingDay(3, 29, 2022), stockB, StockSold(300)),
    )
  )

  List(
    ("before started", TradingDay(9, 1, 2017), Map[Stock,Int]()),
    ("after two purchases", TradingDay(11, 1, 2017), Map(stockA -> 400, stockB -> 100)),
    ("after first sale", TradingDay(3, 30, 2019), Map(stockA -> 100, stockB -> 100)),
    ("day of next purchase", TradingDay(12, 1, 2021), Map(stockA -> 100, stockB -> 100)),
    ("day of split", TradingDay(12, 15, 2021), Map(stockA -> 100, stockB -> 100, stockC -> 200)),
    ("day after split", TradingDay(12, 16, 2021), Map(stockA -> 100, stockB -> 300, stockC -> 200)),
    ("after selling some", TradingDay(3, 30, 2022), Map(stockA -> 100, stockC -> 200)),
  )
    .foreach{ case (reason, day, expectedCounts) =>
      it should s"be correct ${reason} (multiple stocks)" in {
        journalWithMultipleStocks.portfolioAsOf(day) should be (Portfolio(name, expectedCounts))
      }
    }
}
