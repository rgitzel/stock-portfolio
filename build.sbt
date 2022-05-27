ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "stock-portfolio"
  )
  .aggregate(apps, domain, influxdb)

// ====================================================

lazy val apps = (project in file("apps"))
  .settings(
    name := "stock-portfolio-apps"
  )
  .dependsOn(domain, influxdb)

lazy val domain = (project in file("domain"))
  .settings(
    name := "stock-portfolio-domain"
  )

val InfluxDbClientVersion = "6.1.0"

lazy val influxdb = (project in file("impl-influxdb"))
  .settings(
    name := "stock-portfolio-influxdb",
    libraryDependencies ++= Seq(
      "com.influxdb" %% "influxdb-client-scala" % InfluxDbClientVersion,
      "com.influxdb" % "flux-dsl" % InfluxDbClientVersion
    )
  )
  .dependsOn(domain)
