package com.ss.ml.regression

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.LabeledPoint
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.regression.LinearRegression

/**
 * Created by meeraj on 12/10/16.
 */
object MultipleRegression extends App {



  val spark = SparkSession.builder().appName("Regression Model Builder").master("local").getOrCreate()

  import spark.implicits._

  val training = build("kc_house_train_data.csv", "train", spark)
  val test = build("kc_house_test_data.csv", "test", spark)

  spark.sql("select avg(bedrooms_squared), " +
    "avg(bed_bath_rooms), " +
    "avg(log_sqft_living), " +
    "avg(lat_plus_long) from test").show

  val lr = new LinearRegression()

  val m1 = lr.fit(training.map(r => buildLp(r, "sqft_living")))
  println(s"Coefficients: ${m1.coefficients}, Intercept: ${m1.intercept}")
  m1.transform(test.map(r => buildLp(r, "sqft_living"))).show(10)

  val m2 = lr.fit(training.map(r => buildLp(r, "sqft_living", "sqft_living15")))
  println(s"Coefficients: ${m2.coefficients}, Intercept: ${m2.intercept}")
  m2.transform(test.map(r => buildLp(r, "sqft_living", "sqft_living15"))).show(10)


  def build(path: String, view: String, spark: SparkSession) = {

    val toDouble = udf((x: String) => x.toDouble)
    val product = udf((x: Double, y: Double) => x * y)
    val sum = udf((x: Double, y: Double) => x + y)
    val log = udf((x: Double) => scala.math.log(x))

    val df = spark.read.
      option("header", "true").
      csv(path).
      withColumn("sqft_living", toDouble('sqft_living)).
      withColumn("sqft_living15", toDouble('sqft_living15)).
      withColumn("price", toDouble('price)).
      withColumn("bedrooms", toDouble('bedrooms)).
      withColumn("bathrooms", toDouble('bathrooms)).
      withColumn("lat", toDouble('lat)).
      withColumn("long", toDouble('long)).
      withColumn("bedrooms_squared", product('bedrooms, 'bedrooms)).
      withColumn("bed_bath_rooms", product('bedrooms, 'bathrooms)).
      withColumn("lat_plus_long", sum('lat, 'long)).
      withColumn("log_sqft_living", log('sqft_living))

    df.createOrReplaceTempView(view)

    df

  }

  def buildLp(r: Row, input: String*) = {
    var features = input.map(r.getAs[Double](_)).toArray
    new LabeledPoint(r.getAs[Double]("price"), Vectors.dense(features))
  }

}
