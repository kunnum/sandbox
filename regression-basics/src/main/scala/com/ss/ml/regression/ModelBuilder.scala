package com.ss.ml.regression

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

/**
 * Created by meeraj on 09/10/16.
 */
object ModelBuilder extends App {

  val spark = SparkSession.builder().appName("Regression Model Builder").master("local").getOrCreate()

  import spark.implicits._

  val toDouble = udf((x: String) => x.toDouble)
  val prd = udf((x: Double) => x * x)

  val df = spark.read.option("header", "true").csv("sales.csv").
    withColumn("x", toDouble('sqft_living)).withColumn("y", toDouble('price)).select("x", "y")

  df.createOrReplaceTempView("sales")
  val r = spark.sql("select avg(x), avg(y), avg(x * y), avg(x * x) from sales").take(1)

  val avg_x = r(0).getDouble(0)
  val avg_y = r(0).getDouble(1)
  val avg_xy = r(0).getDouble(2)
  val avg_xx = r(0).getDouble(3)

  val s = (avg_xy - avg_x * avg_y) / (avg_xx - avg_x * avg_x)
  val i = avg_y - s * avg_x

  println(s"Slope $s Intercept $i")
  println(i + s * 2650)
  println((800000 - i) / s)

}