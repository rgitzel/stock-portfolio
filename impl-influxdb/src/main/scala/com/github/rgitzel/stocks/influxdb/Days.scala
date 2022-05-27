package com.github.rgitzel.stocks.influxdb

import com.github.rgitzel.stocks.models.TradingDay

import java.time.{Instant, MonthDay, Year, YearMonth, ZoneOffset}

object Days {
  def toInstant(tradingDay: TradingDay): Instant = {
    val monthPrefix = if(tradingDay.month < 10) "0" else ""
    val dayPrefix = if(tradingDay.day < 10) "0" else ""
    val iso8601 = s"${tradingDay.year}-${monthPrefix}${tradingDay.month}-${dayPrefix}${tradingDay.day}T00:00:00.000Z"
    Instant.parse(iso8601)
  }

  def fromInstant(ts: Instant): TradingDay = {
    // ay carumba, do the Java time classes need to be so... surprising?
    //  https://stackoverflow.com/a/27855743/107444
    val localDate = ts.atOffset(ZoneOffset.UTC).toLocalDate()
    TradingDay(
      YearMonth.from(localDate).getMonthValue,
      MonthDay.from(localDate).getDayOfMonth,
      Year.from(localDate).getValue
    )
  }
}
