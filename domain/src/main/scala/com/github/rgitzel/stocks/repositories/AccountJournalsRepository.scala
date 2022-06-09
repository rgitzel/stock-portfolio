package com.github.rgitzel.stocks.repositories

import com.github.rgitzel.stocks.accounts.AccountJournal
import com.github.rgitzel.stocks.models._

import scala.concurrent.{ExecutionContext, Future}

trait AccountJournalsRepository {
  def accountJournals()(implicit ec: ExecutionContext): Future[List[AccountJournal]]
}
