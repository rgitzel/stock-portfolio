package com.github.rgitzel.quicken.transactions

import com.github.rgitzel.quicken.transactions.parsing.QuickenTransactionParser
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.repositories.PortfolioRepository

import java.io.File
import scala.concurrent._
import scala.io.Source
import scala.util.{Failure, Success, Try}

class QuickenTransactionsPortfolioRepository(file: File) extends PortfolioRepository {
  override def portfolioTransactions()(implicit ec: ExecutionContext): Future[List[PortfolioJournal]] =
    Future.fromTry(
      fileLines(file)
        .flatMap{ lines =>
          lines.map(QuickenTransactionParser.fromString).partition(_.isSuccess) match {
            case (parsedLines, Nil) =>
              Success(
                parsedLines.map(_.get)
                  .groupBy(_._1)
                  .map { case (portfolioName, parsed) =>
                    PortfolioJournal(portfolioName, parsed.map(_._2).sortBy(_.tradingDay))
                  }
                  .toList
                  .sortBy(_.name)
              )
            case (_, failures) =>
              val msg = s"failed to read transactions from file '${file.getName}': ${failures.map(_.failed.get.getMessage).mkString(", ")}"
              Failure(new Exception(msg))
          }
        }
    )

  private def fileLines(file: File): Try[List[String]] =
    Try {
      val bufferedSource = Source.fromFile(file)
      val lines = bufferedSource.getLines.toList
      bufferedSource.close
      lines
    }
      .recoverWith(t => Failure(new Exception(s"failed to read file '${file.getName}': ${t.getMessage}")))
}