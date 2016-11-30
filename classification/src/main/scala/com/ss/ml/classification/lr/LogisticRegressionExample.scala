package com.ss.ml.classification.lr

import org.apache.spark.ml.classification.{LogisticRegressionSummary, LogisticRegression}
import org.apache.spark.ml.feature.{Tokenizer, CountVectorizerModel, CountVectorizer}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession

/**
 * Created by meeraj on 17/11/16.
 */
object LogisticRegressionExample extends App {

  val spark = SparkSession.builder().appName("Logistic Regression").master("local").getOrCreate()
  import spark.implicits._

  val df = spark.
    read.
    option("header", "true").
    option("inferSchema", "true").
    option("escape", "\"").
    csv("data/amazon_baby.csv")

  val isGood = udf((x: Int) => if (x >= 4) 1 else 0)

  val tk = new Tokenizer().setInputCol("review").setOutputCol("words")
  val words = tk.transform(df.withColumn("label", isGood('rating)))

  val Array(simpleTraining, simpleTest) = runSimpleModel()
  val simpleModel = new LogisticRegression().fit(simpleTraining)
  val simpleSummary = simpleModel.evaluate(simpleTest)

  val Array(complexTraining, complexTest) = runComplexModel()
  val complexModel = new LogisticRegression().fit(complexTraining)
  val complexSummary = complexModel.evaluate(complexTest)

  val simpleAccuracy = getAccuracy(simpleSummary)
  val complexAccuracy = getAccuracy(complexSummary)

  println(s"Accruracy of simple model is $simpleAccuracy")
  println(s"Accruracy of complex model is $complexAccuracy")

  println("Complex model coefficients: " + complexModel.coefficients)

  def runComplexModel() = {
    val significantWords = Array("love", "great", "easy", "old", "little", "perfect", "loves",
      "well", "able", "car", "broke", "less", "even", "waste", "disappointed",
      "work", "product", "money", "would", "return")
    val cvm = new CountVectorizerModel(significantWords).setInputCol("words").setOutputCol("features")
    cvm.transform(words).randomSplit(Array(0.9, 0.1), 1)
  }

  def runSimpleModel() = {
    val cv = new CountVectorizer().setInputCol("words").setOutputCol("features")
    cv.fit(words).transform(words).randomSplit(Array(0.9, 0.1), 1)
  }

  def getAccuracy(summary: LogisticRegressionSummary) = {
    val incorrectPredictions = summary.predictions.where("label != prediction").count.asInstanceOf[Float]
    val totalPredictions = summary.predictions.count().asInstanceOf[Float]
    val accuracy = incorrectPredictions / totalPredictions
    println(s"Incorrect predictions: $incorrectPredictions, total predictions: $totalPredictions, accuracy: $accuracy")
    accuracy
  }

}
