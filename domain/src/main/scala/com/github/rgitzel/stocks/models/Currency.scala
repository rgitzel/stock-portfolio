package com.github.rgitzel.stocks.models

sealed trait Currency

case object CanadianDollars extends Currency
case object UnitedStatesDollars extends Currency

