package com.ss.scala.spark

import java.util.Date

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

/**
 * Created by meeraj on 10/09/16.
 */
object SensorDataAggregator extends App {

  System.setProperty("spark.cassandra.connection.host", "127.0.0.1")

  val conf = new SparkConf().setMaster("local[2]").setAppName("SnesorDataAggregator")
  val ssc = new StreamingContext(conf, Seconds(10))

  val lines = KafkaUtils.createStream(ssc, "localhost:2181", "weather", Map("weather" -> 1)).map(_._2)
  val pairs = lines.map(_.split(",")).map(r => (r(0), r(2).toDouble))

  import org.apache.spark.HashPartitioner
  import org.apache.spark.util.StatCounter

  val stats = pairs.combineByKey[StatCounter](StatCounter(_), _ merge _, _ merge _, new HashPartitioner(1))

  import BigDecimal.RoundingMode._

  val rec = stats.map { k =>
    val now = new Date()
    (k._1,
    now,
    k._2.count,
    k._2.max,
    BigDecimal(k._2.mean).setScale(2, HALF_UP),
    k._2.min,
    BigDecimal(k._2.stdev).setScale(2, HALF_UP))
  }
  //stats.print

  import com.datastax.spark.connector._

  rec.foreachRDD { rdd =>
    rdd.saveToCassandra("weather", "temperature", SomeColumns("location", "event_time", "temp_count", "temp_max", "temp_mean", "temp_min", "temp_std"))
  }

  ssc.start()
  ssc.awaitTermination()

}
