import sbt._

object Dependencies {
  val InfluxDbClientVersion = "6.1.0"
  val ScalaTestVersion = "3.2.12"

  val ScalaTest = "org.scalatest" %% "scalatest" % ScalaTestVersion % Test

  object InfluxDb {
    val Client = "com.influxdb" %% "influxdb-client-scala" % InfluxDbClientVersion
    val FluxDsl = "com.influxdb" % "flux-dsl" % InfluxDbClientVersion
  }
}


