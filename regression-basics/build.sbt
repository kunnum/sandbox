name := "regression-basics"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "JZY3D" at "http://maven.jzy3d.org/releases"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.0.0"

libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.0.0"

libraryDependencies += "org.apache.spark" % "spark-mllib_2.11" % "2.0.0"

libraryDependencies += "org.jzy3d" % "jzy3d-api" % "1.0.0"
