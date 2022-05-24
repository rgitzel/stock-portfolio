package com.github.rgitzel.stocks.influxdb

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import com.influxdb.client.scala.InfluxDBClientScala
import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux

import java.time.Instant
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class InfluxDbQueryRunner(client: InfluxDBClientScala)(implicit materializer: Materializer) {
  def run[T](query: Flux)(toT: (Instant, Double, Map[String,String]) => T): Future[Seq[T]] = {
    val source =  client.getQueryScalaApi().query(query.toString)

    val flow = Flow[FluxRecord]
      .map{ record =>
        toT(
          record.getTime,
          record.getValue.toString.toDouble,
          record.getValues.asScala.view.mapValues(_.toString).toMap
        )
      }

    val sink = Sink.seq[T]

    source.via(flow).runWith(sink)
  }
}
