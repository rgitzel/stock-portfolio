package com.github.rgitzel.stocks.apps

import com.github.rgitzel.stocks.models.TradingWeek

import java.io.File

object Constants {
  val folder = new File("/Users/rodney/VirtualBox VMs/shared")
  val rawQuickenFilesCad = new File(folder, "investments-cad.txt")
  val rawQuickenFilesUsd = new File(folder, "investments-usd.txt")
  val rawQuickenFiles = List(rawQuickenFilesCad, rawQuickenFilesUsd)
}
