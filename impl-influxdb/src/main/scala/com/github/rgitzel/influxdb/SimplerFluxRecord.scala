package com.github.rgitzel.influxdb

import com.influxdb.query.FluxRecord

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.util.Try

/*
 * the library's class is a bit awkward to deal with
 */
case class SimplerFluxRecord(timestamp: Instant, value: Double, tags: Map[String,String])

object SimplerFluxRecord {
  def apply(r: FluxRecord): SimplerFluxRecord =
    SimplerFluxRecord(
      r.getTime,
      // String? really? could it really be something other than a number??
      Try(r.getValue.toString.toDouble).getOrElse(throw new IllegalStateException(s"wtf?? record has non-numeric value '${r.getValue.toString}")),
      r.getValues.asScala.view.mapValues(_.toString).toMap
    )
}