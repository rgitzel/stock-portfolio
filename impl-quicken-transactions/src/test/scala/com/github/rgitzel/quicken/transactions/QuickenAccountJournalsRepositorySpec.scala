package com.github.rgitzel.quicken.transactions

import com.github.rgitzel.stocks.accounts
import com.github.rgitzel.stocks.accounts.{AccountActivity, AccountJournal, AccountName, StockPurchased, StockSold}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.Currency
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers._

import java.io.File

class QuickenAccountJournalsRepositorySpec extends AsyncFlatSpec with ScalaFutures {
  "portfolioTransactions" should "extract single portfolio with only one transaction" in {
    verifyFromValidFile(
      "single-portfolio-single-transaction",
      List(
        AccountJournal(
          AccountName("TFSA"),
          List(
            AccountActivity(TradingDay(10, 13, 2017), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(100))
          )
        )
      )
    )
  }

  it should "extract single portfolio with multiple transactions" in {
    verifyFromValidFile(
      "single-portfolio",
      List(
        AccountJournal(
          AccountName("TFSA"),
          List(
            accounts.AccountActivity(TradingDay(9, 12, 2017), Stock("TSE:XSP"), Currency("CAD"), StockPurchased(400)),
            accounts.AccountActivity(TradingDay(10, 13, 2017), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(100)),
            accounts.AccountActivity(TradingDay(3, 29, 2019), Stock("TSE:XSP"), Currency("CAD"), StockSold(300)),
            accounts.AccountActivity(TradingDay(12, 1, 2021), Stock("TSE:XIU"), Currency("CAD"), StockPurchased(200)),
          )
        )
      )
    )
  }

  it should "extract three portfolios" in {
    verifyFromValidFile(
      "three-portfolios",
      List(
        AccountJournal(
          AccountName("LIRA"),
          List(
            accounts.AccountActivity(TradingDay(2, 1, 2016), Stock("AAPL"), Currency("USD"), StockPurchased(6))
          )
        ),
        AccountJournal(
          AccountName("RSP"),
          List(
            accounts.AccountActivity(TradingDay(3, 31, 2020), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(5)),
            accounts.AccountActivity(TradingDay(2, 26, 2021), Stock("TSE:XTR"), Currency("CAD"), StockPurchased(1))
          ),
        ),
        AccountJournal(
          AccountName("TFSA"),
          List(
            accounts.AccountActivity(TradingDay(9, 12, 2017), Stock("TSE:XSP"), Currency("CAD"), StockPurchased(400)),
            accounts.AccountActivity(TradingDay(10, 13, 2017), Stock("TSE:XCS"), Currency("CAD"), StockPurchased(100)),
            accounts.AccountActivity(TradingDay(3, 29, 2019), Stock("TSE:XSP"), Currency("CAD"), StockSold(300)),
            accounts.AccountActivity(TradingDay(12, 1, 2021), Stock("TSE:XIU"), Currency("CAD"), StockPurchased(200))
          )
        )
      )
    )
  }

  def verifyFromValidFile(name: String, portfolios: List[AccountJournal]) = {
    val file = new File(this.getClass.getClassLoader.getResource(s"transaction-files/${name}.txt").toURI)
    new QuickenAccountJournalsRepository(file).accountJournals().futureValue should be (portfolios)
  }
}