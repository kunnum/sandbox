package com.ss.ml.regression

import org.apache.spark.sql.{Row, SparkSession}

/**
 * Created by meeraj on 13/02/17.
 */
object GradientDescent extends App {

  println("Gradient descent start")

  val spark = SparkSession.builder().appName("Gradient Descent").master("local").getOrCreate()
  import spark.implicits._

  val df = spark.read.option("header", "true").
    option("inferSchema", "true").csv("profit.txt")
  df.cache

  val size = df.count
  val alpha = 0.01

  val (t1, t2) = gradientDescent(0.0, 0.0, 1500)
  println(s"h(x) = $t1 + $t2 * x")

  def gradientDescent(t1: Double, t2: Double, it: Int): (Double, Double) = it match {
    case 0 => (t1, t2)
    case _ =>
      val x = t1 - df.map(pdTheta0(t1, t2)).reduce(_ + _) * alpha / size
      val y = t1 - df.map(pdTheta1(t1, t2)).reduce(_ + _) * alpha / size
      val c = cost(x, y)
      if (it % 50 == 0) println(s"Intercept: $x, Gradient: $y, Iteration: $it, Cost: $c")
      gradientDescent(x, y, it - 1)
  }

  def pdTheta0(t1: Double, t2: Double): (Row) => Double = {
    r => {
      val x1 = r.getAs[Double]("x1")
      val y = r.getAs[Double]("y")
      t1 + x1 * t2 - y
    }
  }

  def pdTheta1(t1: Double, t2: Double): (Row) => Double = {
    r => {
      val x1 = r.getAs[Double]("x1")
      val y = r.getAs[Double]("y")
      (t1 + x1 * t2 - y) * x1
    }
  }

  def cost(t1: Double, t2: Double) = {
    df.map { r =>
      val x1 = r.getAs[Double]("x1")
      val y = r.getAs[Double]("y")
      math.pow(t1 + x1 * t2 - y, 2)
    }.reduce(_ + _) / 2 * size
  }
}
