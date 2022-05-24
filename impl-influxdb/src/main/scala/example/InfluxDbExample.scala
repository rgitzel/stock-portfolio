package example

import com.influxdb.client.scala.InfluxDBClientScalaFactory

trait InfluxDbExample {
  val influxDBClient = InfluxDBClientScalaFactory
    .create("http://192.168.0.17:8086", "".toCharArray, "foo")
}
