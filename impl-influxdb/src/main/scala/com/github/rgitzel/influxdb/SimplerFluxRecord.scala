package com.github.rgitzel.influxdb

import com.influxdb.query.FluxRecord

import java.time.Instant
import scala.jdk.CollectionConverters._

/*
 * the library's class is a bit awkward to deal with
 */
case class SimplerFluxRecord(timestamp: Instant, value: Double, tags: Map[String,String])

object SimplerFluxRecord {
  def apply(r: FluxRecord): SimplerFluxRecord =
    SimplerFluxRecord(
      r.getTime,
      // String??
      r.getValue.toString.toDouble,
      r.getValues.asScala.view.mapValues(_.toString).toMap
    )
}