package com.github.rgitzel.quicken.transactions

import com.github.rgitzel.quicken.transactions.parsing.QuickenReportLines
import com.github.rgitzel.stocks.accounts.AccountJournal
import com.github.rgitzel.stocks.repositories.AccountJournalsRepository

import java.io.File
import scala.concurrent._
import scala.util._

class QuickenAccountJournalsRepository(files: List[File])
    extends AccountJournalsRepository {
  override def accountJournals()(implicit
      ec: ExecutionContext
  ): Future[List[AccountJournal]] =
    Future.fromTry(
      files.map(readQuickenFile).partition(_.isSuccess) match {
        case (successes, Nil) =>
          accountJournalsFromLines(successes.flatMap(_.get))
        case (_, failures) =>
          val filenames = files.map(_.getName).mkString(", ")
          val exceptions = failures.map(_.failed.get).mkString("\n  ")
          Failure(
            new Exception(
              s"failed to read transactions from files '${filenames}':\n  ${exceptions}"
            )
          )
      }
    )

  def accountJournalsFromLines(lines: List[String]): Try[List[AccountJournal]] =
    QuickenReportLines.extractActivities(lines).partition(_.isSuccess) match {
      case (parsedLines, failures) =>
        println("Ignoring:")
        failures.foreach(f => println(s"  ${f.failed.get.getMessage}"))

        Success(
          parsedLines
            .map(_.get)
            .groupBy(_._1)
            .map { case (portfolioName, parsed) =>
              val activities = parsed.map(_._2).sortBy(_.tradingDay)
              AccountJournal(portfolioName, activities)
            }
            .toList
            .sortBy(_.name)
        )
    }

  def readQuickenFile(file: File): Try[List[String]] = {
    FileUtils
      .fileLines(file)
      .map { lines =>
        println(s"read ${lines.size} lines")
        lines
      }
  }
}
