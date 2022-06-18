package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.accounts.{AccountActivity, AccountName, AccountTransaction}

import scala.collection.immutable.::
import scala.util.{Success, Try}

object QuickenReportLines {

  def extractActivities(exportedLines: List[String]): List[Try[(AccountName,AccountActivity)]] = {
    processRawLines(exportedLines).map(QuickenTransactionParser.fromString)
  }

  val toBeRemoved = List(
    ",",
    " - Action Direct",
    " - Cdn",
    ".000"
  )

  // my version of Quicken refuses to put _symbols_ in the reports, just the names :-(
  val stockSymbolsForNames = List(
    ("Alphabet - Class A", "GOOGL"),
    ("Alphabet - Class C", "GOOG"),
    ("Amazon", "AMZN"),
    ("Apple", "AAPL"),
    ("Berkshire Hathaway - Class B", "BRK.B"),
    ("Cameco Corp", "TSE:CCO"),
    ("iShares Core S&P 500 Index ETF (CAD-Hedged)", "TSE:XSP"),
    ("iShares Core S&P/TSX Capped Composite Index ETF", "TSE:XIC"),
    ("iShares Diversified Monthly Income ETF", "TSE:XTR"),
    ("iShares Global Agriculture Index ETF", "TSE:COW"),
    ("iShares India Index ETF", "TSE:XID"),
    ("ISHARES S&P/TSX 60 INDEX FUND", "TSE:XIU"),
    ("iShares S&P/TSX Composite High Dividend Index ETF", "TSE:XEI"),
    ("iShares S&P/TSX Global Base Metals Index ETF", "TSE:XBM"),
    ("ISHARES S&P/TSX SMALLCAP INDEX FUND", "TSE:XCS"),
    ("iShares Core S&P Total US Stock Market ETF", "ITOT"),
    ("Netflix", "NFLX")
  ).toMap

  def processRawLines(lines: List[String]): List[String] = {
    val x = lines
      .filter(_.trim.nonEmpty)
      .filterNot(line =>
        line.contains("TOTAL")
          || line.contains("BALANCE")
          || line.contains("through")
          || line.contains("Account")
      )
      .map(
        _.split('\t')
          .toList
      )

    val y = x.map(list =>
      list.slice(1, list.size - 1) // first is always empty (?) and last is some weird number
    )
    val useful =  y.filter(_.nonEmpty)
//    useful.foreach{ xs =>
//        println(s"${xs.size}: '${xs.mkString("|")}'")
//      }

    val squished = useful.foldLeft(List[List[String]]()) { case (lines, line) =>
      if (line.head == "")
        (lines.head ++ line) +: lines.tail
      else
        line +: lines
    }
      .reverse
    //        squished.foreach{ xs =>
    //          println(s"${xs.size}: '${xs.mkString("|")}'")
    //        }

    val cleaned = squished
      .map { values =>
        values
          .map { individualValue =>
            toBeRemoved.foldLeft(individualValue) { case (value, removeThis) =>
              value.replace(removeThis, "")
            }
          }
//          .filter(_.nonEmpty)
      }

    val brokenUp = cleaned
      .flatMap { parseAction}

    brokenUp.map(_.mkString("|"))
  }

  // "TFSA - USD" -> ("TFSA", "USD")
  def splitAccountAndCurrency(s: String): (String,String) = {
    s.split(" - ", 2).toList match {
      case account :: currency :: Nil =>
        (account, currency)
      case _ =>
        throw new Exception(s"invalid account '${s}'")
    }
  }

  def parseAction(x: List[String]) =
    x match {
      case date :: accountAndCurrency :: action :: stockName :: arguments =>
        val symbol = stockSymbolsForNames.getOrElse(stockName, stockName.replaceAll(" ", ""))
        val (account, currency) = splitAccountAndCurrency(accountAndCurrency)
        (action, arguments) match {
          // buy
          case ("Bought" | "Sold", price :: amount :: commission :: total :: Nil) =>
            Some(List(account, currency, symbol, date, action, amount, price, commission, total, "0.0"))
          // sales almost always have a second line, and sometimes purchases do too
          case ("Bought" | "Sold", price :: amount :: commission :: total :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: adjustment :: Nil) =>
            Some(List(account, currency, symbol, date, action, amount, price, commission, total, adjustment))
          // transfer _shares_ from another account
          case ("Added", _ :: amount :: _ :: _ :: Nil) =>
            Some(List(account, currency, symbol, date, action, amount))
          case ("Added" | "Removed", _ :: amount :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: Nil) =>
            Some(List(account, currency, symbol, date, action, amount))
          // stock split
          case ("StkSplit", _ :: ratio :: _ :: _ :: Nil) =>
            Some(List(account, currency, symbol, date, action, ratio))
          // dividends
          case ("CGShort" | "CGMid" | "CGLong" | "Div", _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: amount :: Nil) =>
            Some(List(account, currency, symbol, date, action, amount))
          // interest and other cash things that use two lines... TODO: model these separately?
          case ("IntInc" | "MiscExp" | "MiscInc", _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: amount :: Nil) =>
            Some(List(account, currency, symbol, date, action, amount))
          // these use just a single line
          case ("Cash" | "XIn" | "XOut", _ :: _ :: _ :: amount :: Nil) =>
            Some(List(account, currency, symbol, date, action, amount))
          case ("Reminder", _) =>
            None
          case _ =>
            println(s"ignoring $x because $arguments aren't recognized")
            None
        }
      case _ =>
        println(s"ignoring $x")
        None
    }
}
