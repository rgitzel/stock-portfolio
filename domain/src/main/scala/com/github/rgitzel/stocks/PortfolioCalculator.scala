package com.github.rgitzel.stocks

import com.github.rgitzel.stocks.accounts.{
  Account,
  AccountCalculator,
  AccountJournal,
  AccountValuation
}
import com.github.rgitzel.stocks.models._
import com.github.rgitzel.stocks.money.{Currency, MonetaryValue, MoneyConverter}
import com.github.rgitzel.stocks.repositories.{
  ForexRepository,
  IntegrityChecker,
  PricesRepository
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PortfolioCalculator(
    forexRepository: ForexRepository,
    pricesRepository: PricesRepository,
    totalsCurrency: Currency
) {
  def valuate(
      week: TradingWeek,
      accountJournals: List[AccountJournal]
  )(implicit ec: ExecutionContext): Future[PortfolioValuation] = {
    val currenciesAcrossAllPortfolios =
      accountJournals.flatMap(_.currencies).distinct

    forexRepository.weeklyClosingRates(week).flatMap {
      exchangeRatesForThisWeek =>
        pricesRepository.weeklyClosingPrices(week).flatMap {
          pricesForStocksThisWeek =>
            if (exchangeRatesForThisWeek.isEmpty)
              Future
                .failed(new Exception(s"no exchange rates found for ${week}"))
            else if (pricesForStocksThisWeek.isEmpty)
              Future.failed(new Exception(s"no stock prices found for ${week}"))
            else
              Future.fromTry(
                portfolioValuationForThisWeek(
                  week,
                  accountJournals.map(_.accountAsOf(week.friday)),
                  new MoneyConverter(exchangeRatesForThisWeek),
                  pricesForStocksThisWeek,
                  currenciesAcrossAllPortfolios,
                  totalsCurrency
                )
              )
        }
    }
  }

  private def portfolioValuationForThisWeek(
      week: TradingWeek,
      accountsForThisWeek: List[Account],
      moneyConverter: MoneyConverter,
      pricesForStocksThisWeek: Map[Stock, MonetaryValue],
      currenciesAcrossAllPortfolios: List[Currency],
      totalsCurrency: Currency
  )(implicit ec: ExecutionContext): Try[PortfolioValuation] = {
    // we want the closing price of each stock in _all_ currencies (it's possible to have, say,
    //  AAPL in a Canadian dollar account)
    val closingPrices = pricesForStocksThisWeek.flatMap {
      case (stock, publishedMonetaryValue) =>
        currenciesAcrossAllPortfolios.flatMap { currency =>
          moneyConverter
            .convert(publishedMonetaryValue, currency)
            .map(ClosingPrice(stock, _))
        }
    }.toList

    IntegrityChecker.checkForMissingPrices(
      accountsForThisWeek,
      closingPrices
    ) match {
      case Nil =>
        // these are in the currency of the respective account
        val accountValuations =
          accountsForThisWeek.map(AccountCalculator.valuate(_, closingPrices))

        // this total is in the "desired" currency
        val totalValue = accountValuations.flatMap { accountValuation =>
          accountValuation.valuesForStocksForCurrency
            .flatMap { case (currency, holdings) =>
              val subtotal = MonetaryValue(holdings.values.sum, currency)
              moneyConverter.convert(subtotal, totalsCurrency)
            }
            .map(_.value)
        }.sum

        Success(
          PortfolioValuation(
            MonetaryValue(totalValue, totalsCurrency),
            accountValuations
          )
        )
      case errors =>
        // TODO: better combination of errors
        Failure(new Exception(s"$week - ${errors.mkString(", ")}"))
    }
  }

  def valuate(weeks: List[TradingWeek], accountJournals: List[AccountJournal])(
      implicit ec: ExecutionContext
  ): Future[List[(TradingWeek, PortfolioValuation)]] =
    Future
      .sequence(
        weeks.map { week =>
          valuate(week, accountJournals)
            // wrap in `Option` so that we don't have to make a fake `PortfolioValuation` just for the `recoverWith` case
            .map(valuation => Some((week, valuation)))
            .recoverWith { t =>
              println(
                s"WARNING! skipping ${week} due to errors: ${t.getMessage} "
              )
              Future.successful(None)
            }
        }
      )
      .map(_.flatten)
}
