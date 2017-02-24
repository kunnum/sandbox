import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.ALS
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Created by meeraj on 20/02/17.
 */
object RecommenderPrimer extends App {

  val spark = SparkSession.builder().appName("Recommender Basics").master("local[4]").getOrCreate()
  import spark.implicits._

  case class Rating(userId: Int, movieId: Int, rating: Float, timestamp: Long)

  def parseRating(str: String): Rating = {
    val fields = str.split("::")
    Rating(fields(0).toInt, fields(1).toInt, fields(2).toFloat, fields(3).toLong)
  }

  val df = spark.read.textFile("/Users/meeraj/dss-data/movies.txt")
  val ratings = df.map(s => parseRating(s)).toDF()

  val Array(training, test) = ratings.randomSplit(Array(0.8, 0.2))

  val als = new ALS().setMaxIter(5).setRegParam(0.01).setUserCol("userId").setItemCol("movieId").setRatingCol("rating")

  val model = als.fit(training)

  model.transform(training).show

}
