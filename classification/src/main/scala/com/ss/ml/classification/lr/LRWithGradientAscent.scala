package com.ss.ml.classification.lr

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{CountVectorizerModel, Tokenizer}
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Row, DataFrame, SparkSession}

/**
 * Created by meeraj on 26/11/16.
 */
object LRWithGradientAscent extends App {

  val spark = SparkSession.builder().appName("Logistic Regression").master("local").getOrCreate()
  import spark.implicits._

  val removePunctuations = udf((x: String) => x.replaceAll("[^a-zA-Z0-9\\s]", ""))

  var df =
    spark.
    read.
    option("header", "true").
    option("inferSchema", "true").
    option("escape", "\"").
    csv("data/amazon_baby_subset.csv").
    withColumn("review", removePunctuations('review)).
    withColumn("constant", lit(1))

  val words =
    spark.
    read.
    csv("data/important_words.json").
    collect().
    map(_.getAs[String](0))

  val tok = new Tokenizer().setInputCol("review").setOutputCol("words")
  val cvm = new CountVectorizerModel(words).setInputCol("words").setOutputCol("features")
  val pl = new Pipeline().setStages(Array(tok, cvm))
  val withFeatures = pl.setStages(Array(tok, cvm)).fit(df).transform(df)
  val cfs = logisticRegression(withFeatures, List.fill(words.length + 1)(0.0), 1e-7, 301)

  println(cfs)

  /*
   * Compute the scores for a given set of coefficients.
   */
  def computeScores(df: DataFrame, cfs: List[Double]) = {
    val scoreUdf = udf(
      (sv: SparseVector) => {
        val l = for {
          i <- 0 until sv.indices.length
          index = sv.indices(i)
          cf = cfs(index + 1)
          v = sv.values(i)
        } yield (cf * v)
        cfs(0) + (if (l.isEmpty) 0 else l.reduce(_ + _))
      }
    )
    df.withColumn("score", scoreUdf('features))
  }

  /*
   * Compute the probability based on the current score.
   */
  def computeProbabilities(df: DataFrame) = {
    val probabilityUdf = udf(
      (s: Double) => {
        1 / (1 + Math.pow(Math.E, -s))
      }
    )
    df.withColumn("probability", probabilityUdf('score))
  }

  /*
   * Compute error based on indicator function and current probability.
   */
  def computeErrors(df: DataFrame) = {
    val errorUdf = udf(
      (s: Int, p: Double) => {
        val i = if (s == 1) 1 else 0
        i - p
      }
    )
    df.withColumn("error", errorUdf('sentiment, 'probability))
  }

  /*
   * Compute partial derivative for a given coefficient.
   */
  def computeFeatureDerivative(df: DataFrame, idx: Int) = {
    val derivativeUdf = udf(
      (sv: SparseVector, e: Double) => {
        if (idx == 0) {
          e
        } else {
          sv.indices.find(_ == idx) match {
            case Some(i) => sv.values(i - 1) * e
            case None => 0
          }
        }
      }
    )
    df.withColumn("derivative", derivativeUdf('features, 'error)).agg(sum('derivative)).first.getDouble(0)
  }

  /*
   * Compute the log likelihood.
   */
  def computeLogLikelihood(df: DataFrame) = {
    val lleUdf = udf(
      (s: Int, sc: Double) => {
        val i = if (s == 1) 1 else 0
        i * sc - Math.log(1.0 + math.pow(math.E, -sc))
      }
    )
    df.withColumn("lle", lleUdf('sentiment, 'score)).agg(sum('lle)).first.getDouble(0)
  }

  /*
   * Performs logistic regression.
   */
  def logisticRegression(df: DataFrame, cfs: List[Double], stepSize: Double, iters: Int) = {
    var ret = cfs
    for (i <- 0 to iters) {
      val withScore = computeScores(df, ret)
      val withProbability = computeProbabilities(withScore)
      val withError = computeErrors(withProbability)
      for (j <- 0 to ret.length) {
        val der = computeFeatureDerivative(withError, j)
        ret = ret.updated(j, ret(j) + stepSize * der)
      }
      val lp = computeLogLikelihood(withError)
      if (i % 100 == 0) println(s"Log likelihood on $i iteration is $lp")
    }
    ret
  }

}
