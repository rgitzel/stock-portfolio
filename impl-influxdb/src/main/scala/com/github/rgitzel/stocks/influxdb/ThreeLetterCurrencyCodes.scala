package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.stocks.models.{CanadianDollars, Currency, UnitedStatesDollars}

object ThreeLetterCurrencyCodes {
  def toCurrency(code: String): Currency =
    code match {
      case "USD" => UnitedStatesDollars
      case "CAD" => CanadianDollars
      case _ => throw new Exception(s"need to handle bad currency '${code}")
    }
}
