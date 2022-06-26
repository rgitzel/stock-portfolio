package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.accounts.AccountValuation
import com.github.rgitzel.stocks.money.MonetaryValue

case class PortfolioValuation(
    // TODO: something better than "desired"
    totalValueInDesiredCurrency: MonetaryValue,
    accounts: List[AccountValuation]
)
