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

lazy val influxdb = (project in file("impl-influxdb"))
  .settings(
    name := "stock-portfolio-influxdb",
    libraryDependencies ++= Seq(
      "com.influxdb" %% "influxdb-client-scala" % "6.0.0",
      "com.influxdb" % "flux-dsl" % "6.0.0"
    )
  )
  .dependsOn(domain)
