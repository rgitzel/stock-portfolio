package com.github.rgitzel.quicken.transactions

import com.github.rgitzel.stocks.accounts
import com.github.rgitzel.stocks.accounts.{AccountActivity, AccountJournal, AccountName, StockPurchased, StockSold}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.Currency
import org.scalatest.TryValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers._

import java.io.File

class QuickenAccountJournalsRepositorySpec extends AsyncFlatSpec with ScalaFutures with TryValues {
  "portfolioTransactions" should "extract single portfolio with only one transaction and with commission" in {
    verifyFromLines(
      List(
        "TFSA CAD TSE:XCS 10/13/17 Bought 100 2.0 3.0"
      ),
      List(
        AccountJournal(
          AccountName("TFSA"),
          List(
            AccountActivity(TradingDay(10, 13, 2017), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(100, 2.0, 3.0))
          )
        )
      )
    )
  }

  it should "extract single portfolio with only one transaction but without commission" in {
    verifyFromLines(
      List(
        "TFSA CAD TSE:XCS 10/13/17 Bought 100 2.0 "
      ),
      List(
        AccountJournal(
          AccountName("TFSA"),
          List(
            AccountActivity(TradingDay(10, 13, 2017), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(100, 2.0, 0.0))
          )
        )
      )
    )
  }

  it should "extract single portfolio with multiple transactions" in {
    verifyFromLines(
      List(
        "TFSA CAD TSE:XCS 10/13/17 Bought 100 4.0",
        "TFSA CAD TSE:XIU 12/01/21 Bought 200 3.0",
        "TFSA CAD TSE:XSP 3/29/19 Sold -300 2.0 2.1",
        "TFSA CAD TSE:XSP 9/12/17 Bought 400 1.0 1.1"
      ),
      List(
        AccountJournal(
          AccountName("TFSA"),
          List(
            accounts.AccountActivity(TradingDay(9, 12, 2017), Stock("TSE:XSP"), Currency("CAD"), StockPurchased(400, 1.0, 1.1)),
            accounts.AccountActivity(TradingDay(10, 13, 2017), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(100, 4.0, 0.0)),
            accounts.AccountActivity(TradingDay(3, 29, 2019), Stock("TSE:XSP"), Currency("CAD"), StockSold(300, 2.0, 2.1)),
            accounts.AccountActivity(TradingDay(12, 1, 2021), Stock("TSE:XIU"), Currency("CAD"), StockPurchased(200, 3.0, 0.0)),
          )
        )
      )
    )
  }

  it should "extract three portfolios" in {
    verifyFromLines(
      List(
        "TFSA CAD TSE:XCS 10/13/17 Bought 100 0.1 10.1",
        "RSP CAD TSE:XTR 2/26/21 Bought 1 0.001",
        "TFSA CAD TSE:XIU 12/01/21 Bought 200 0.2 10.2",
        "TFSA CAD TSE:XSP 3/29/19 Sold -300 0.3 10.3",
        "LIRA USD AAPL 2/01/16 Bought 6 0.006",
        "TFSA CAD TSE:XSP 9/12/17 Bought 400 0.4 10.4",
        "RSP CAD TSE:XCS 3/31/20 Bought 5 0.005"
      ),
      List(
        AccountJournal(
          AccountName("LIRA"),
          List(
            accounts.AccountActivity(TradingDay(2, 1, 2016), Stock("AAPL"), Currency("USD"), StockPurchased(6, 0.006, 0.0))
          )
        ),
        AccountJournal(
          AccountName("RSP"),
          List(
            accounts.AccountActivity(TradingDay(3, 31, 2020), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(5, 0.005, 0.0)),
            accounts.AccountActivity(TradingDay(2, 26, 2021), Stock("TSE:XTR"), Currency("CAD"), StockPurchased(1, 0.001, 0.0))
          ),
        ),
        AccountJournal(
          AccountName("TFSA"),
          List(
            accounts.AccountActivity(TradingDay(9, 12, 2017), Stock("TSE:XSP"), Currency("CAD"), StockPurchased(400, 0.4, 10.4)),
            accounts.AccountActivity(TradingDay(10, 13, 2017), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(100, 0.1, 10.1)),
            accounts.AccountActivity(TradingDay(3, 29, 2019), Stock("TSE:XSP"), Currency("CAD"), StockSold(300, 0.3, 10.3)),
            accounts.AccountActivity(TradingDay(12, 1, 2021), Stock("TSE:XIU"), Currency("CAD"), StockPurchased(200, 0.2, 10.2))
          )
        )
      )
    )
  }

  def verifyFromLines(lines: List[String], expectedPortfolios: List[AccountJournal]) = {
    new QuickenAccountJournalsRepository(null).accountJournalsFromLines(lines).get should be (expectedPortfolios)
  }
}
