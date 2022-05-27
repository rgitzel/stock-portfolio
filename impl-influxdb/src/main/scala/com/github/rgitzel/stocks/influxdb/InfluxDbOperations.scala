package com.github.rgitzel.stocks.influxdb

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.influxdb.client.scala.InfluxDBClientScala
import com.influxdb.client.write.Point
import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

/*
 * pulling records out of Influx seems pretty consistent, all that differs
 *  is the query (of course) and a way to convert a record into some type T
 */
class InfluxDbOperations(client: InfluxDBClientScala)(implicit materializer: Materializer) {
  def runQuery[T](query: Flux)(toT: (Instant, Double, Map[String,String]) => T)(implicit ec: ExecutionContext): Future[Seq[T]] = {
    val startedAt = Instant.now
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

    source.via(flow).runWith(sink).andThen {
      case Failure(_) =>
        println(s"InfluxDb query failed after ${elapsedMillis(startedAt)}ms")
      case Success(_) =>
        println(s"InfluxDb query succeeded after ${elapsedMillis(startedAt)}ms")
    }
  }

  def write(bucket: String, point: Point)(implicit ec: ExecutionContext): Future[Done] = {
    val startedAt = Instant.now

    val sourcePoint = Source.single(point)
    val sinkPoint = client.getWriteScalaApi.writePoint(Some(bucket))
    val materializedPoint = sourcePoint.toMat(sinkPoint)(Keep.right)

    materializedPoint.run().andThen {
      case Failure(_) =>
        println(s"InfluxDb write failed after ${elapsedMillis(startedAt)}ms")
      case Success(_) =>
        println(s"InfluxDb write succeeded after ${elapsedMillis(startedAt)}ms")
    }
  }

  private def elapsedMillis(startedAt: Instant): Long =
    Duration.between(startedAt, Instant.now).toMillis
}
