name := "spark-kafka"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.10.0.0"

libraryDependencies += "org.apache.spark" % "spark-streaming-kafka-0-8_2.11" % "2.0.0"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.0.0"

libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.0.0"

libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.0.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "2.4.7"

libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7"

libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.0-M3"
