package cpm.ss.ml.regression.basics

import org.apache.spark.ml.feature.LabeledPoint
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.sql.{Row, SparkSession}

/**
 * Created by meeraj on 28/09/16.
 */
object HousePricePredictor extends App {


  val spark = SparkSession.builder().appName("Hose price predictor").master("local[1]").getOrCreate()
  import spark.implicits._

  val df = spark.read.option("header", "true").option("inferSchema", "true").csv("home_data.csv")
  df.printSchema()

  val simple = (r : Row) => new LabeledPoint(r.getInt(2), Vectors.dense(r.getInt(5)))
  val Array(trainingSimple, testSimple) = df.map(simple).randomSplit(Array(0.8, 0.2), 1)

  val lr = new LinearRegression()
  val simpleModel = lr.fit(trainingSimple)
  simpleModel.transform(trainingSimple).show(10)

  /*val simple = (r : Row) => new LabeledPoint(r.getInt(2), Vectors.dense(r.getInt(5)))
  val complex = (r : Row) => new LabeledPoint(r.getInt(2), Vectors.dense(r.getInt(3), r.getDouble(4), r.getInt(5), r.getInt(6)))

  val Array(trainingSimple, testSimple) = df.map(simple).randomSplit(Array(0.8, 0.2), 1)
  val Array(trainingComplex, testComplex) = df.map(complex).randomSplit(Array(0.8, 0.2), 1)

  val lr = new LinearRegression()

  val simpleModel = lr.fit(trainingSimple)
  val complexModel = lr.fit(trainingComplex)

  simpleModel.transform(trainingSimple).show(10)
  complexModel.transform(trainingComplex).show(10)*/


}
