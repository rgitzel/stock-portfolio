package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.MonetaryValue

import scala.concurrent.{ExecutionContext, Future}

trait PortfolioValueRepository {
  def updateValue(day: TradingDay, portfolioName: PortfolioName, stock: Stock, value: MonetaryValue)(implicit ec: ExecutionContext): Future[Unit]

  def updateValues(week: TradingWeek, valuations: List[PortfolioValuation])(implicit ec: ExecutionContext): Future[Unit] = {
    val combinedPortfolioUpdates = valuations.flatMap { portfolioValuation =>
      portfolioValuation.valuesForStocksForCurrency.flatMap { case (currency, holdings) =>
        holdings
          .toList
          .sortBy(_._1.symbol)
          .map { case (stock, value) =>
            (week, portfolioValuation.name, stock, MonetaryValue(value, currency))
          }
      }
    }
    combinedPortfolioUpdates.foreach(println)
    Future.sequence(
      combinedPortfolioUpdates
        .map { case (week, portfolioName, stock, value) =>
          updateValue(week.lastDay, portfolioName, stock, value)
        }
    ).map(_ => ())
  }
}
