package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.accounts._
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.Currency
import org.scalatest.TryValues
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

import scala.util.Success

class QuickenReportLinesTest extends AnyFlatSpecLike with TryValues {

  val secondLinePrefix = "\t" * 8

  "extractActivities" should "extract a stock split" in {
    val lines = List(
      "\t9/02/20\tRSP - CAD - Action Direct\tStkSplit\tApple - Cdn\t\t1,668:417\t\t\t1.00"
    )
    val expected = List(
      Success((AccountName("RSP"), AccountActivity(TradingDay(9, 2, 2020),Stock("AAPL"),Currency("CAD"),StockSplit(4))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a buy with commission" in {
    val lines = List(
      "\t10/13/17\tTFSA - CAD - Action Direct\tBought\tISHARES S&P/TSX SMALLCAP INDEX FUND\t16.230\t70.000\t9.95\t-1,146.05\t1.00"
    )
    val expected = List(
      Success((AccountName("TFSA"), AccountActivity(TradingDay(10, 13, 2017),Stock("TSE:XCS"),Currency("CAD"),StockPurchased(70,16.23,9.95))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a buy without commission" in {
    val lines = List(
      "\t8/13/20\tRSP - CAD - Action Direct\tBought\tISHARES S&P/TSX 60 INDEX FUND\t25.350\t4.000\t\t-101.40\t1.00"
    )
    val expected = List(
      Success((AccountName("RSP"), AccountActivity(TradingDay(8, 13, 2020),Stock("TSE:XIU"),Currency("CAD"),StockPurchased(4,25.35,0.0))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a purchase with an adjustment" in {
    val lines = List(
      "\t4/29/15\tRSP - CAD - Action Direct\tBought\tCanadian Oils Sands TR\t30.000\t162.000\t\t639.70\t1.00",
      s"${secondLinePrefix}-5,499.70\t1.00"
    )
    val expected = List(
      Success((AccountName("RSP"), AccountActivity(TradingDay(4, 29, 2015),Stock("CanadianOilsSandsTR"),Currency("CAD"),StockPurchased(162,30.0,0.0))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a sell with commission" in {
    val lines = List(
      "\t8/21/08\tRSP - CAD - Action Direct\tSold\tCanadian Oils Sands TR\t51.700\t-100.000\t28.95\t3,140.48\t1.00",
      s"${secondLinePrefix}2,000.57\t1.00"
    )
    val expected = List(
      Success((AccountName("RSP"), AccountActivity(TradingDay(8, 21, 2008),Stock("CanadianOilsSandsTR"),Currency("CAD"),StockSold(100,51.7,28.95))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a sell without a price or commission (likely delisted)" in {
    val lines = List(
      "\t12/31/07\tINVEST - CAD - Action Direct\tSold\tAxion Spatial Imaging\t\t-1,500.000\t\t711.46\t1.00",
      s"${secondLinePrefix}-711.46\t1.00"
    )
    val expected = List(
      Success((AccountName("INVEST"), AccountActivity(TradingDay(12, 31, 2007),Stock("AxionSpatialImaging"),Currency("CAD"),StockSold(1500,0.00, 0.00))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a dividend" in {
    val lines = List(
      "\t8/13/20\tRSP - CAD - Action Direct\tDiv\tApple - Cdn\t\t\t\t\t1.00",
      s"${secondLinePrefix}451.17\t1.00"
    )
    val expected = List(
      Success((AccountName("RSP"), AccountActivity(TradingDay(8, 13, 2020),Stock("AAPL"),Currency("CAD"),Dividend(451.17))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a deposit" in {
    val lines = List(
      "\t12/01/21\tTFSA - CAD - Action Direct\tXIn\t-Cash-\t\t\t\t50.00\t1.00"
    )
    val expected = List(
      Success((AccountName("TFSA"), AccountActivity(TradingDay(12, 1, 2021),Stock("-Cash-"),Currency("CAD"),Deposit(50.0))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a withdrawal" in {
    val lines = List(
      "\t6/19/00\tINVEST - CAD - Action Direct\tXOut\t-Cash-\t\t\t\t-1,500.00\t1.00"
    )
    val expected = List(
      Success((AccountName("INVEST"), AccountActivity(TradingDay(6, 19, 2000),Stock("-Cash-"),Currency("CAD"),Withdrawal(1500.0))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract interest paid" in {
    val lines = List(
      "\t12/31/99\tRSP - CAD - Action Direct\tIntInc\t-Cash-\t\t\t\t\t1.00",
      s"${secondLinePrefix}0.18\t1.00\t"
    )
    val expected = List(
      Success((AccountName("RSP"), AccountActivity(TradingDay(12, 31, 1999),Stock("-Cash-"),Currency("CAD"),Deposit(0.18))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract an 'Added''" in {
    val lines = List(
      "\t2/04/02\tRSP - CAD - Action Direct\tAdded\tIBM - Cdn\t116.000\t33.000\t\t-3,828.00\t1.00",
      s"${secondLinePrefix}3,828.00\t1.00\t"
    )
    val expected = List(
      Success((AccountName("RSP"), AccountActivity(TradingDay(2, 4, 2002),Stock("IBM"),Currency("CAD"),StockPurchased(33, 0.0, 0.0))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }

  it should "extract a 'Removed''" in {
    val lines = List(
      "\t2/03/03\tINVEST - CAD - Action Direct\tRemoved\tIBM - Cdn\t\t-43.000\t\t3,870.00\t1.00",
      s"${secondLinePrefix}-3,870.00\t1.00\t"
    )
    val expected = List(
      Success((AccountName("INVEST"), AccountActivity(TradingDay(2, 3, 2003),Stock("IBM"),Currency("CAD"),StockSold(43, 0.0, 0.0))))
    )
    QuickenReportLines.extractActivities(lines) should be (expected)
  }
}
