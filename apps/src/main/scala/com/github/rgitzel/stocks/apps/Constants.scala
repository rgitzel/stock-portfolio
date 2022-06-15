package com.github.rgitzel.stocks.apps

import com.github.rgitzel.stocks.models.TradingWeek

import java.io.File

object Constants {
  val folder = new File("/Users/rodney/VirtualBox VMs/shared")
  val rawQuickenFilesCad = new File(folder, "investments-cad.txt")
  val rawQuickenFilesUsd = new File(folder, "investments-usd.txt")
  val rawQuickenFiles = List(rawQuickenFilesCad, rawQuickenFilesUsd)

  val lastTradingWeek2017 = TradingWeek(12, 29, 2017)
  val lastTradingWeek2018 = TradingWeek(12, 28, 2018)
  val lastTradingWeek2019 = TradingWeek(12, 27, 2019)
}
