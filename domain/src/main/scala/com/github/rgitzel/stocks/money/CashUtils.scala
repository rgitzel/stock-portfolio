package com.github.rgitzel.stocks.money

object CashUtils {
  // use this to avoid the occasional floating-point weirdness
  //  c.f. https://stackoverflow.com/questions/11106886/scala-doubles-and-precision
  def roundedToCents(d: Double) = "%.2f".format(d).toDouble
}
