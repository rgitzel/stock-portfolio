package com.github.rgitzel.influxdb

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.influxdb.client.scala.InfluxDBClientScala
import com.influxdb.client.write.Point
import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/*
 * hide the Akka streams handling, it doesn't change
 *  based on the queries or updates themselves
 */
class InfluxDbOperations(client: InfluxDBClientScala)(implicit materializer: Materializer) {
  // pulling records out of Influx seems pretty consistent, all that differs
  //  is the query (of course) and a way to convert a record into some type T
  def runQuery[T](query: Flux)(convertToDesiredType: SimplerFluxRecord => T)(implicit ec: ExecutionContext): Future[Seq[T]] = {
    val startedAt = Instant.now
    client.getQueryScalaApi().query(query.toString)
      .via(
        Flow[FluxRecord]
          .map(SimplerFluxRecord(_))
          .map(convertToDesiredType)
      )
      .runWith(Sink.seq[T])
      .andThen {
        case Failure(_) =>
          log(logMessageForQuery("failed", startedAt))
        case Success(_) =>
          log(logMessageForQuery("succeeded", startedAt))
      }
  }

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

  // =============================

  // TODO: build a simple logger trait
  private def log(s: String) = {
//    println(s)
  }

  private def logMessageForQuery(result: String, startedAt: Instant) =
    s"InfluxDb query ${result} after ${elapsedMillis(startedAt)}ms"

  private def logMessageForWrite(result: String, points: List[Point], startedAt: Instant) =
    s"InfluxDb write of ${points.size} points ${result} after ${elapsedMillis(startedAt)}ms"

  private def elapsedMillis(startedAt: Instant): Long =
    Duration.between(startedAt, Instant.now).toMillis
}
