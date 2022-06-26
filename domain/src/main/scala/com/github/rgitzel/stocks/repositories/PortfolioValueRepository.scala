package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.accounts.AccountName
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class AccountSingleStockValuationRecord(
    day: TradingDay,
    portfolioName: AccountName,
    stock: Stock,
    value: MonetaryValue
)
final case class AccountTotalValuationRecord(
    day: TradingDay,
    portfolioName: AccountName,
    value: MonetaryValue
)

// this is for the portfolio overall
final case class PortfolioValuationRecord(day: TradingDay, value: MonetaryValue)

trait PortfolioValueRepository {
  def updateAccountStockRecords(
      records: List[AccountSingleStockValuationRecord]
  )(implicit
      ec: ExecutionContext
  ): Future[Int]
  def updatePortfolioRecords(records: List[PortfolioValuationRecord])(implicit
      ec: ExecutionContext
  ): Future[Int]
  def updateAccountRecords(records: List[AccountTotalValuationRecord])(implicit
      ec: ExecutionContext
  ): Future[Int]

  def updateValues(
      records: List[WeeklyRecords]
  )(implicit ec: ExecutionContext): Future[Int] =
    for {
      stocksCount <- updateAccountStockRecords(records.flatMap(_.accountStocks))
        .andThen {
          case Success(count) =>
            println(s"successfully wrote ${count} stocks records")
          case Failure(t) =>
            println(s"failed to write stocks results: ${t.getMessage}")
        }
      subtotalsCount <- updateAccountRecords(records.flatMap(_.accounts))
        .andThen {
          case Success(count) =>
            println(s"successfully wrote ${count} subtotals records")
          case Failure(t) =>
            println(s"failed to write subtotals results: ${t.getMessage}")
        }
      totalsCount <- updatePortfolioRecords(records.map(_.portfolio))
        .andThen {
          case Success(count) =>
            println(s"successfully wrote ${count} totals records")
          case Failure(t) =>
            println(s"failed to write totals results: ${t.getMessage}")
        }
    } yield (stocksCount + subtotalsCount + totalsCount)

}
