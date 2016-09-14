package com.ss.scala.spark

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import com.datastax.spark.connector._

/**
 * Created by meeraj on 10/09/16.
 */
object ReadingsAggregator extends App {

  System.setProperty("spark.cassandra.connection.host", "127.0.0.1")

  val conf = new SparkConf().setMaster("local[2]").setAppName("readings-data-aggregator")
  val ssc = new StreamingContext(conf, Seconds(5))

  val host = "localhost:2181"
  val topic = "readings"

  val lines = KafkaUtils.createStream(ssc, host, topic, Map(topic -> 1)).map(_._2)
  val pairs = lines.map(l => l.split(",")).map(a => a(0) -> a(2).toDouble)
  val rec = pairs.reduceByKey((a, x) => a + x).foreachRDD { rdd =>
    rdd.saveToCassandra("demo", "power_usage", SomeColumns("location", "usage"))
  }

  ssc.start()
  ssc.awaitTermination()

}
