package com.ss.ml.regression


import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.functions._
/**
 * Created by meeraj on 09/10/16.
 */
object DataCleanser extends App {

  val spark = SparkSession.builder().appName("Regression Model Builder").master("local").getOrCreate()

  import spark.implicits._

  val toDouble = udf((x: String) => x.toDouble)

  val train = spark.read.option("header", "true").csv("sales.csv")
  train.withColumn("price", toDouble('price)).select("price", "sqft_living").show

}
