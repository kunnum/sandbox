package com.ss.ml.clustering

import java.util.Date

import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.ml.feature.{IDF, Tokenizer, HashingTF}
import org.apache.spark.ml.linalg.Vector

object ClusteringBasics extends App {

  val spark = SparkSession.builder().appName("Clustering Basics").master("local[4]").getOrCreate()
  val df = spark.read.option("header", "false").csv("data")

  import spark.implicits._

  val tk = new Tokenizer().setInputCol("_c2").setOutputCol("words")
  val tf = new HashingTF().setInputCol("words").setOutputCol("tf")
  val idf = new IDF().setInputCol("tf").setOutputCol("tf-idf")

  val terms = tf.transform(tk.transform(df))
  val idfs = idf.fit(terms).transform(terms)

  def getTfIdf(uri: String, ds: DataFrame) = {
    val r = ds.filter(s"_c0 = '$uri'").take(1)
    r(0).getAs[Vector]("tf-idf")
  }

  val obamaTfIdf = getTfIdf("<http://dbpedia.org/resource/Barack_Obama>", idfs)
  val clintonTfIdf = getTfIdf("<http://dbpedia.org/resource/Bill_Clinton>", idfs)
  val beckhamfIdf = getTfIdf("<http://dbpedia.org/resource/David_Beckham>", idfs)

  def dotProduct(v1: Vector, v2: Vector) = {
    var dp = 0.0
    var index = v1.size - 1
    for (i <- 0 to index) {
      dp += v1(i) * v2(i)
    }
    dp
  }

  def nearestNeighbour() = {
    val x = idfs.map(r => (r.getString(1), dotProduct(obamaTfIdf, r.getAs[Vector]("tf-idf"))))
    import org.apache.spark.sql.functions._
    x.sort(desc("_2")).show(10)
  }

  def similarity() = {
    println("Similarity metric between Obama and Clinton is " + dotProduct(obamaTfIdf, clintonTfIdf))
    println("Similarity metric between Obama and Beckham is " + dotProduct(obamaTfIdf, beckhamfIdf))
  }

  def cluster() = {
    val Array(training, test) = idfs.randomSplit(Array(0.8, 0.2), 1)
    val model = new KMeans().setFeaturesCol("tf-idf").setK(5).setSeed(1).fit(training)
    model.transform(test).show(10)
  }

}
