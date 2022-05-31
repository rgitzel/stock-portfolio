package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._

import scala.concurrent.{ExecutionContext, Future}

trait PortfolioRepository {
  def portfolioTransactions()(implicit ec: ExecutionContext): Future[List[PortfolioJournal]]
}
