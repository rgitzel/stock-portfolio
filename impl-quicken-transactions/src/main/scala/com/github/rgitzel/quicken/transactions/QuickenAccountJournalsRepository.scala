package com.github.rgitzel.quicken.transactions

import com.github.rgitzel.quicken.transactions.parsing.QuickenTransactionParser
import com.github.rgitzel.stocks.accounts.AccountJournal
import com.github.rgitzel.stocks.repositories.AccountJournalsRepository

import java.io.File
import scala.concurrent._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class QuickenAccountJournalsRepository(files: List[File]) extends AccountJournalsRepository {
  override def accountJournals()(implicit ec: ExecutionContext): Future[List[AccountJournal]] =
    Future.fromTry(
      files.map(readQuickenFile).partition(_.isSuccess) match {
        case (successes, Nil) =>
          accountJournalsFromLines(successes.map(_.get).flatten)
        case (_, failures) =>
          val filesnames = files.map(_.getName).mkString(", ")
          val exceptions = failures.map(_.failed.get).mkString("\n  ")
          Failure(new Exception(s"failed to read transactions from files '${filesnames}':\n  ${exceptions}"))
      }
    )

  def accountJournalsFromLines(lines: List[String]): Try[List[AccountJournal]] =
    lines.map(QuickenTransactionParser.fromString).partition(_.isSuccess) match {
      case (parsedLines, Nil) =>
        Success(
          parsedLines.map(_.get)
            .groupBy(_._1)
            .map { case (portfolioName, parsed) =>
              AccountJournal(portfolioName, parsed.map(_._2).sortBy(_.tradingDay))
            }
            .toList
            .sortBy(_.name)
        )
      case (_, failures) =>

        Failure(new Exception(s"\n${failures.map(_.failed.get.getMessage).mkString("\n  ")}"))
    }

  def readQuickenFile(file: File): Try[List[String]] = {
    FileUtils.fileLines(file)
      .flatMap{ lines =>
        println(s"read ${lines.size} lines")

        val columns = 10

        val useful = lines.map(_.split('\t').toList)
          .map(list => list.slice(1, list.size-1)) // first is always empty (?) and last is some weird number
          .filter(_.nonEmpty)
          .tail // skip the heading line
//        useful.foreach{ xs =>
//            println(s"${xs.size}: '${xs.mkString("|")}'")
//          }

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

        val toBeRemoved = List(
          ",",
          " - Action Direct",
          ".000"
        )

        // my version of Quicken refuses to put _symbols_ in the reports, just the names :-(
        val replacements = List(
          ("Alphabet - Class A", "KEEP-GOOGL"),
          ("Alphabet - Class C", "KEEP-GOOG"),
          ("Amazon", "KEEP-AMZN"),
          ("Apple - Cdn", "KEEP-AAPL"),
          ("Apple", "KEEP-AAPL"),
          ("Berkshire Hathaway - Class B", "KEEP-BRK.B"),
          ("Cameco Corp", "KEEP-TSE:CCO"),
          ("IBM - Cdn", "KEEP-IBM"),
          ("iShares Core S&P 500 Index ETF (CAD-Hedged)", "KEEP-TSE:XSP"),
          ("iShares Core S&P/TSX Capped Composite Index ETF", "KEEP-TSE:XIC"),
          ("iShares Diversified Monthly Income ETF", "KEEP-TSE:XTR"),
          ("iShares Global Agriculture Index ETF", "KEEP-TSE:COW"),
          ("iShares India Index ETF", "KEEP-TSE:XID"),
          ("ISHARES S&P/TSX 60 INDEX FUND", "KEEP-TSE:XIU"),
          ("iShares S&P/TSX Composite High Dividend Index ETF", "KEEP-TSE:XEI"),
          ("iShares S&P/TSX Global Base Metals Index ETF", "KEEP-TSE:XBM"),
          ("ISHARES S&P/TSX SMALLCAP INDEX FUND", "KEEP-TSE:XCS"),
          ("iShares Core S&P Total US Stock Market ETF", "KEEP-ITOT"),
          ("Netflix", "KEEP-NFLX")
        )
        
        val cleaned = squished
          .map{ values =>
            values
              .map{ individualValue =>
                toBeRemoved.foldLeft(individualValue){ case (value, removeThis) =>
                  value.replace(removeThis, "")
                }
              }
              .map{ individualValue =>
                replacements.foldLeft(individualValue){ case (value, (replaceThis, withThis)) =>
                  value.replace(replaceThis, withThis)
                }
              }
              .filter(_.nonEmpty)
          }
          .filter(
            _.find(s =>
              s.contains("KEEP") || s.contains("Cash")
            ).isDefined
          )
          .map(
            _.map(
              _.replace("KEEP-", "")
            )
          )
//        cleaned.foreach{ xs =>
//            println(s"${xs.size}: '${xs.mkString("|")}'")
//        }

        val brokenUp = cleaned.map{ values =>
          values.flatMap(_.split(" - ").toList)
        }.flatMap { x =>
          x match {
            // buy or sell with commission
            case List(date, account, currency, action, symbol, price, amount, commission, cash) =>
              Some(List(account, currency, symbol, date, action, amount, price, commission))
            // sell with commission and some other weird value
            case List(date, account, currency, action, symbol, price, amount, commission, cash, _) =>
              Some(List(account, currency, symbol, date, action, amount, price, commission))
            // buy without commission (usually re-investing dividends)
            case List(date, account, currency, action, symbol, price, amount, _) =>
              Some(List(account, currency, symbol, date, action, amount, price))
            // stock split, dividends
            case List(date, account, currency, action, symbol, amount) =>
              Some(List(account, currency, symbol, date, action, amount))
            case _ =>
              println(s"ignoring $x")
              None
          }
        }
//        brokenUp.foreach{ xs =>
//          println(xs.mkString(" "))
//        }

        Success(brokenUp.map(_.mkString(" ")))
      }
  }
}