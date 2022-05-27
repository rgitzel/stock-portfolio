package com.github.rgitzel.influxdb

import akka.actor.ActorSystem
import com.influxdb.client.scala.{InfluxDBClientScala, InfluxDBClientScalaFactory}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait InfluxDbApp extends App {

  def useInfluxDbClient(influxDBClient: InfluxDBClientScala)(implicit ec: ExecutionContext): Future[_]

  // =======================================

  implicit val system: ActorSystem = ActorSystem("influxdb-app", None, None, Some(global))

  val influxDBClient = InfluxDBClientScalaFactory
    .create("http://192.168.0.17:8086", "".toCharArray, "foo")

  try {
    if(!influxDBClient.ping) {
      println("failed to connect!")
    }
    else {
      println("connected successfully")

      val result = useInfluxDbClient(influxDBClient)(global)
  //    .andThen {
  //    case Success(_) =>
  //      println("finished successfully")
  //    case Failure(t) =>
  //      println(s"failed on ${t.getMessage}")
  //  }

      Await.result(result, 10.seconds)
    }
  }
  finally {
    influxDBClient.close()
    system.terminate()
  }
}
