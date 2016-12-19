package com.ss.ml.classification.lr

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

/**
 * Created by meeraj on 10/12/16.
 */
object DecisionTreeExample extends App {

  val spark = SparkSession.builder().appName("Logistic Regression").master("local[4]").getOrCreate()
  import spark.implicits._

  var i = 0

  val index = udf {
    (x: Int) => i
  }
  val safeLoans = udf {
    (x: Int) => if (x == 0) +1 else -1
  }

  val df =
    spark.
      read.
      option("header", "true").
      option("inferSchema", "true").
      option("escape", "\"").
      csv("data/dectree/data").
      withColumn("safe_loans", safeLoans('bad_loans)).withColumn("id", monotonically_increasing_id)

  val loans = df.select("row_number(), id, grade, sub_grade, short_emp, emp_length_num, home_ownership, dti, purpose, term, " +
    "last_delinq_none, last_major_derog_none, revol_util, total_rec_late_fee, safe_loans")

  loans.show()
  
}
