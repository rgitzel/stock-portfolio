package com.github.rgitzel.stocks.money

import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers._

class MoneyConverterSpec extends AnyFlatSpecLike with OptionValues {
  val currencyA = Currency("A")
  val currencyB = Currency("B")
  val currencyC = Currency("C")

  val converter = new MoneyConverter(
    Map(
      ConversionCurrencies(currencyA, currencyB) -> 2.0
    )
  )

  val moneyA = MonetaryValue(1.0, currencyA)
  val moneyB = MonetaryValue(2.0, currencyB)

  "convert" should "apply multiplier if conversion exists" in {
    converter.convert(moneyA, currencyB).value should be (moneyB)
  }

  "convert" should "apply multiplier if _reverse_ conversion exists" in {
    converter.convert(moneyB, currencyA).value should be (moneyA)
  }

  "convert" should "do nothing if no conversion available" in {
    converter.convert(moneyB, currencyC) should be (None)
  }
}
