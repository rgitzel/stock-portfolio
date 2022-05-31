
import Dependencies.{InfluxDb, _}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "stock-portfolio"
  )
  .aggregate(apps, domain, influxdb, quickenTransactions)

// ====================================================

lazy val apps = (project in file("apps"))
  .settings(
    name := "stock-portfolio-apps"
  )
  .dependsOn(domain, influxdb, quickenTransactions)

lazy val domain = (project in file("domain"))
  .settings(
    name := "stock-portfolio-domain",
    libraryDependencies ++= Seq(
      ScalaTest
    )
  )

lazy val influxdb = (project in file("impl-influxdb"))
  .settings(
    name := "stock-portfolio-influxdb",
    libraryDependencies ++= Seq(
      InfluxDb.Client,
      InfluxDb.FluxDsl
    )
  )
  .dependsOn(domain)

lazy val quickenTransactions = (project in file("impl-quicken-transactions"))
  .settings(
    name := "stock-portfolio-quicken-transactions",
    libraryDependencies ++= Seq(
      ScalaTest
    )
  )
  .dependsOn(domain)
