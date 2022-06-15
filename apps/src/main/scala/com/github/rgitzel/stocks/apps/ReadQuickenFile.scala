package com.github.rgitzel.stocks.apps

import com.github.rgitzel.quicken.transactions.QuickenAccountJournalsRepository

import java.io.File
import scala.util.control.NonFatal

object ReadQuickenFile extends App {
  new QuickenAccountJournalsRepository(List())
    .readQuickenFile(new File("/Users/rodney/VirtualBox VMs/shared/export test.TXT"))
    .recover{ case NonFatal(t) => println(t.getMessage) }
}
