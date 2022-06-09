package com.github.rgitzel.stocks.models

import com.github.rgitzel.stocks.accounts.AccountValuation
import com.github.rgitzel.stocks.money.MonetaryValue

case class PortfolioValuation(
                               totalValue: MonetaryValue,
                               accounts: List[AccountValuation]
                             )
