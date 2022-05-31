package com.github.rgitzel.stocks.models

case class PortfolioName(s: String)

object PortfolioName {
  implicit val ordering: Ordering[PortfolioName] = new Ordering[PortfolioName] {
    override def compare(x: PortfolioName, y: PortfolioName): Int = x.s.compare(y.s)
  }
}