package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.models._

import scala.concurrent.{ExecutionContext, Future}

trait PortfolioJournalsRepository {
  def portfolioJournals()(implicit ec: ExecutionContext): Future[List[PortfolioJournal]]
}
