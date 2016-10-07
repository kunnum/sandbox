package com.ss.ml.classification

import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{Tokenizer, CountVectorizerModel, CountVectorizer}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession


/**
 * Created by meeraj on 03/10/16.
 */
object ClassificationPrimer extends App {

  val spark = SparkSession.builder().appName("Classification Primer").master("local").getOrCreate()
  import spark.implicits._

  val df = spark.read.option("header", "true").option("inferSchema", "true").csv("amazon_baby.csv")
  df.show(10)

  val keywords = Array("awesome",
    "great",
    "fantastic",
    "amazing",
    "love",
    "horrible",
    "bad",
    "terrible",
    "awful",
    "wow",
    "hate")

  val filterWords = udf(
    (x: String) =>
      if (x != null)
        x.split(" ").filter(keywords.contains(_)).mkString(" ")
      else ""
  )
  val isGood = udf((x: Int) => if (x >= 4) 1 else 0)

  val data = df.where("rating != 3").withColumn("label", isGood('rating)).
    withColumn("cleansed", filterWords('review)).
    where("cleansed != ''")
  data.select("name", "cleansed", "label").show(10)

  val classifier = new LogisticRegression()

  val tokenizer = new Tokenizer().
    setInputCol("cleansed").
    setOutputCol("words")
  val cvm = new CountVectorizerModel(keywords).
    setInputCol("words").
    setOutputCol("features")

  val Array(training, test) = cvm.transform(
    tokenizer.transform(data)).
    randomSplit(Array(0.8, 0.2), 1)

  val model = classifier.fit(training)
  model.evaluate(test).predictions.select("words", "label", "prediction", "probability").show(10)


}
