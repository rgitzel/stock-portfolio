package com.github.rgitzel.quicken.transactions.parsing

import com.github.rgitzel.stocks.models.TradingDay

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

object TradingDayParser {
  // e.g. "2/11/21"
  def fromString(s: String): Try[TradingDay] = {
    s.split('/').toList match {
      case List(month, day, year) =>
        Try(TradingDay(month.toInt, day.toInt, ("20" + year).toInt))
          .recoverWith { _ => failure(s) }
      case _ =>
        failure(s)
    }
  }

  def failure(s: String) = Failure(new IllegalArgumentException(s"invalid trading day string '${s}'"))
}
