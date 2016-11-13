package com.ss.ml.regression

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

/**
 * Created by meeraj on 09/10/16.
 */
object MultiRegressionModelBuilder extends App {

  val spark = SparkSession.builder().appName("Regression Model Builder").master("local").getOrCreate()

  import spark.implicits._

  val df = build("kc_house_train_data.csv", "train", spark)

  val (features, labels) = buildMatrix(df, Array("sqft_living"), "price")

  predictOutcome(features, Array(1, 2)).take(10).foreach(println _)

  def build(path: String, view: String, spark: SparkSession) = {

    val toDouble = udf((x: String) => x.toDouble)
    val product = udf((x: Double, y: Double) => x * y)
    val sum = udf((x: Double, y: Double) => x + y)
    val log = udf((x: Double) => scala.math.log(x))

    spark.read.
      option("header", "true").
      csv(path).
      //withColumn("constant", lit(1.0)).
      withColumn("sqft_living", toDouble('sqft_living)).
      withColumn("price", toDouble('price)).
      withColumn("bedrooms", toDouble('bedrooms)).
      withColumn("bathrooms", toDouble('bathrooms)).
      withColumn("lat", toDouble('lat)).
      withColumn("long", toDouble('long)).
      withColumn("bedrooms_squared", product('bedrooms, 'bedrooms)).
      withColumn("bed_bath_rooms", product('bedrooms, 'bathrooms)).
      withColumn("lat_plus_long", sum('lat, 'long)).
      withColumn("log_sqft_living", log('sqft_living))

  }

  def buildMatrix(df: DataFrame, featureColumns: Array[String], labelColumn: String) = {
    val features = df.rdd.map(r => Vectors.dense(1.0, featureColumns.map(r.getAs[Double](_)):_*))
    val labels = df.rdd.map(r => Vectors.dense(r.getAs[Double](labelColumn)))
    (new RowMatrix(features, features.count(), 2), new RowMatrix(labels, labels.count(), 1))
  }

  def predictOutcome(features: RowMatrix, weights: Array[Double]) = {
    features.rows.map{f =>
      val pairs : Seq[Double] = for (i <- 0 to weights.length - 1) yield f(i) * weights(i)
      pairs.reduce((a, b) => a + b)
    }
  }

  def featureDerivative(features: RowMatrix, errors: RowMatrix) = {
    val featureScalar = features.rows.map(_(0))
    val errorScalar = errors.rows.map(_(0))
    featureScalar.zip(errorScalar).map(a => a._1 * a._2 * 2).reduce((a, b) => a + b)
  }

  /*
  def regression_gradient_descent(feature_matrix, output, initial_weights, step_size, tolerance):
    converged = False
    weights = np.array(initial_weights) # make sure it's a numpy array
    while not converged:
        # compute the predictions based on feature_matrix and weights using your predict_output() function
        predictions = predict_output(feature_matrix, weights)
        # compute the errors as predictions - output
        errors = predictions - output
        gradient_sum_squares = 0 # initialize the gradient sum of squares
        # while we haven't reached the tolerance yet, update each feature's weight
        for i in range(len(weights)): # loop over each weight
            # Recall that feature_matrix[:, i] is the feature column associated with weights[i]
            # compute the derivative for weight[i]:
            derivative = feature_derivative(errors, feature_matrix[:,i])
            # add the squared value of the derivative to the gradient sum of squares (for assessing convergence)
            gradient_sum_squares += derivative**2
            # subtract the step size times the derivative from the current weight
            weights[i] -= derivative * step_size
        # compute the square-root of the gradient sum of squares to get the gradient matnigude:
        gradient_magnitude = sqrt(gradient_sum_squares)
        if gradient_magnitude < tolerance:
            converged = True
    return(weights)
   */

}