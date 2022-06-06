package com.github.rgitzel.stocks.money

class MoneyConverter(conversions: Map[ConversionCurrencies,Double]) {
  def convert(money: MonetaryValue, to: Currency): Option[MonetaryValue] = {
    val from = money.currency
    if(from == to) {
      // nothing to do
      Some(money)
    } else {
      multiplier(from, to).map(m => MonetaryValue(m * money.value, to))
    }
  }

  private def multiplier(from: Currency, to: Currency): Option[Double] = {
    val conversion = ConversionCurrencies(from, to)
    (conversions.get(conversion), conversions.get(conversion.reverse)) match {
      case (Some(multiplier), _) =>
        Some(multiplier)
      case (None, Some(multiplier)) =>
        // we can convert the other way, but need to take the inverse
        Some(1.0 / multiplier)
      case _ =>
        None
    }
  }

}
