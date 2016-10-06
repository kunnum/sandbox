package com.ss.ml.classification

import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{Tokenizer, CountVectorizerModel, CountVectorizer}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StringType


/**
 * Created by meeraj on 03/10/16.
 */
object ClassificationPrimer extends App {

  val spark = SparkSession.builder().appName("Classification Primer").master("local").getOrCreate()
  import spark.implicits._

  val df = spark.read.option("header", "true").option("inferSchema", "true").csv("amazon_baby.csv")

  /*val keywords = Array("awesome", "great", "fantastic", "amazing", "love", "horrible", "bad", "terrible", "awful", "wow", "hate")



  val filterWords = udf((x: String) => if (x != null) x.split(" ").filter(keywords.contains(_)).mkString(" ") else "")
  val isGood = udf((x: Int) => if (x >= 4) 1 else 0)

  val data = df.where("rating != 3").withColumn("label", isGood('rating)).withColumn("cleansed", filterWords('review)).where("cleansed != ''")

  val classifier = new LogisticRegression()

  val tokenizer = new Tokenizer().setInputCol("cleansed").setOutputCol("words")
  val cvm = new CountVectorizerModel(keywords).setInputCol("words").setOutputCol("features")

  val Array(training, test) = cvm.transform(tokenizer.transform(data)).randomSplit(Array(0.8, 0.2), 1)

  val model = classifier.fit(training)
  model.evaluate(test).predictions.select("words", "label", "prediction").show(100)*/

  val count = udf((x: String, w: String) => if (x != null) x.split(" ").filter(_ == w).length else 0)
  df.withColumn("awesome", count('review, lit("awesome")))
    .withColumn("great", count('review, lit("great")))
    .withColumn("fantastic", count('review, lit("fantastic")))
    .withColumn("amazing", count('review, lit("amazing")))
    .withColumn("love", count('review, lit("love")))
    .withColumn("horrible", count('review, lit("horrible")))
    .withColumn("bad", count('review, lit("bad")))
    .withColumn("terrible", count('review, lit("terrible")))
    .withColumn("awful", count('review, lit("awful")))
    .withColumn("wow", count('review, lit("wow")))
    .withColumn("hate", count('review, lit("hate"))).createOrReplaceTempView("reviews")

  spark.sql("select sum(awesome), sum(great), sum(fantastic), sum(amazing), sum(love), sum(horrible), sum(bad), sum(terrible), sum(awful), sum(wow), sum(hate) from reviews").show()

  val keywords = Array("awesome", "great", "fantastic", "amazing", "love", "horrible", "bad", "terrible", "awful", "wow", "hate")



  val filterWords = udf((x: String) => if (x != null) x.split(" ").filter(keywords.contains(_)).mkString(" ") else "")
  val isGood = udf((x: Int) => if (x >= 4) 1 else 0)

  val data = df.where("rating != 3").withColumn("label", isGood('rating)).withColumn("cleansed", filterWords('review)).where("cleansed != ''")

  val classifier = new LogisticRegression()

  val tokenizer = new Tokenizer().setInputCol("cleansed").setOutputCol("words")
  val cvm = new CountVectorizerModel(keywords).setInputCol("words").setOutputCol("features")

  val Array(training, test) = cvm.transform(tokenizer.transform(data)).randomSplit(Array(0.8, 0.2), 0)

  val model = classifier.fit(training)
  println(model.coefficients)

  model.evaluate(test).predictions.createOrReplaceTempView("predictions")
  spark.sql("select * from predictions").show
  spark.sql("select count(*) from predictions").show
  spark.sql("select count(*) from predictions where label = prediction").show


}
