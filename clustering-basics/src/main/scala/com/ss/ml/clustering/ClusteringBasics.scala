package com.ss.ml.clustering

import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.ml.feature.{IDF, Tokenizer, HashingTF}
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.functions._

object ClusteringBasics extends App {

  val spark = SparkSession.builder().appName("Clustering Basics").master("local[4]").getOrCreate()
  val df = spark.read.option("header", "true").csv("people.csv")

  import spark.implicits._

  val tk = new Tokenizer().setInputCol("_c2").setOutputCol("words")
  val tf = new HashingTF().setInputCol("words").setOutputCol("tf")
  val idf = new IDF().setInputCol("tf").setOutputCol("tf-idf")

  val terms = tf.transform(tk.transform(df))
  val idfs = idf.fit(terms).transform(terms)

  def getTfIdf(name: String, ds: DataFrame) = {
    val r = ds.filter(s"_c1 = '$name'").take(1)
    r(0).getAs[Vector]("tf-idf")
  }

  def dotProduct(v1: Vector, v2: Vector) = {
    var dp = 0.0
    var index = v1.size - 1
    for (i <- 0 to index) {
      dp += v1(i) * v2(i)
    }
    dp
  }

  def cluster(idfs: DataFrame, k: Int) = {
    val model = new KMeans().setFeaturesCol("tf-idf").setK(k).setSeed(1).fit(idfs)
    model.transform(idfs).sort("prediction").select("_c1", "prediction").show(10, false)
  }

  cluster(idfs, 3)

  def similarity(source: String, target: String, idfs: DataFrame) = {
    val sourceTfIdf = getTfIdf(source, idfs)
    val targetfIdf = getTfIdf(target, idfs)
    val dp = dotProduct(sourceTfIdf, targetfIdf)
    println(s"Similarity metric between $source and $target is $dp")
  }

  similarity("Barack Obama", "Joe Biden", idfs)
  similarity("Barack Obama", "David Beckham", idfs)
  similarity("Barack Obama", "Tony Blair", idfs)

  def nearestNeighbour(idfs: DataFrame, name: String) = {
    val sourceTfIdf = getTfIdf(name, idfs)
    val x = idfs.map(r => (r.getString(1), dotProduct(sourceTfIdf, r.getAs[Vector]("tf-idf"))))
    val neighbour = x.sort(desc("_2")).select("_1").take(2)(1).getAs[String](0)
    println(s"Nearest neighbour for $name is $neighbour")
  }

  nearestNeighbour(idfs, "Barack Obama")

}
