package com.github.rgitzel.influxdb

import java.time.Instant

/*
 * this should be enough to look up the offending record with a query like the following:
 *
 *  > select * from forex where time=1651795200000000000
 */
class MissingTagsException(measurement: String, timestamp: Instant, tags: Iterable[String], requiredTags: List[String])
  extends Exception(s"missing required tags (${requiredTags.mkString(", ")}) from measurement '${measurement}' at ${timestamp.toEpochMilli * 1000000}: ${tags.mkString(", ")}")
