package example

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.influxdb.client.scala.InfluxDBClientScalaFactory
import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux

import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object InfluxDB2ScalaExample {

  implicit val system: ActorSystem = ActorSystem("it-tests")

  def main(args: Array[String]): Unit = {

    val influxDBClient = InfluxDBClientScalaFactory
      .create("http://192.168.0.17:8086", "my-token".toCharArray, "my-org")

//    val fluxQuery = (
//      """from(bucket: "stocks")
//           |> range(start: -7d)
//      """
//    )

    //      + " |> filter(fn: (r) => (r[\"_measurement\"] == \"cpu\" and r[\"_field\"] == \"usage_system\"))"

    val mem = Flux.from("stocks")
      .range(-10L, ChronoUnit.DAYS)
//      .filter(Restrictions.and(Restrictions.measurement().equal("mem"), Restrictions.field().equal("used_percent")))
    val fluxQuery = mem.toString

    //Result is returned as a stream
    val results = influxDBClient.getQueryScalaApi().query(fluxQuery)

    //Example of additional result stream processing on client side
    val sink = results
      //filter on client side using `filter` built-in operator
//      .filter(it => "cpu0" == it.getValueByKey("cpu"))
      //take first 20 records
      .take(20)
      //print results
      .runWith(Sink.foreach[FluxRecord](it => println(s"Measurement: ${it.getMeasurement}, value: ${it.getValues}")
      ))

    // wait to finish
    Await.result(sink, Duration.Inf)

    influxDBClient.close()
    system.terminate()
  }
}
