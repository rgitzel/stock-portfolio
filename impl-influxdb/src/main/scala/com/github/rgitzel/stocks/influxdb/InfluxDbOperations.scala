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
        log(logMessageForQuery("failed", startedAt))
      case Success(_) =>
        log(logMessageForQuery("succeeded", startedAt))
    }
  }

  private def logMessageForQuery(result: String, startedAt: Instant) =
    s"InfluxDb query ${result} after ${elapsedMillis(startedAt)}ms"

  def write(bucket: String, points: List[Point])(implicit ec: ExecutionContext): Future[Done] = {
    val startedAt = Instant.now

    Source.single(points)
      .toMat(client.getWriteScalaApi.writePoints(Some(bucket)))(Keep.right)
      .run()
      .andThen {
        case Failure(_) =>
          log(logMessageForWrite("failed", points, startedAt))
        case Success(_) =>
          log(logMessageForWrite("succeeded", points, startedAt))
      }
  }

  def write(bucket: String, point: Point)(implicit ec: ExecutionContext): Future[Done] =
    write(bucket, List(point))

  private def log(s: String) = {
//    println(s)
  }
  
  private def logMessageForWrite(result: String, points: List[Point], startedAt: Instant) =
    s"InfluxDb write of ${points.size} points ${result} after ${elapsedMillis(startedAt)}ms"

  private def elapsedMillis(startedAt: Instant): Long =
    Duration.between(startedAt, Instant.now).toMillis
}
