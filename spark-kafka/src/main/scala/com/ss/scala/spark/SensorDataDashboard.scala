package com.ss.scala.spark

import java.sql.Timestamp
import java.util.{Date, Properties}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.datastax.driver.core.{Row, Cluster}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.collection.mutable.ListBuffer
import scala.io.{Source, StdIn}

/**
  * Created by meeraj on 10/09/16.
  */
object SensorDataDashboard extends App {

  System.setProperty("spark.cassandra.connection.host", "127.0.0.1")

  implicit val system = ActorSystem("sensor-data-dashboard")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val html = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("dashboard.html")).mkString
  val cluster = Cluster.builder().addContactPoint("localhost").withPort(9042).build()

  val route =
    get {
      path("") {
        val session = cluster.connect()
        val rows = session.execute("select event_time, temp_min, temp_mean, temp_max from weather.temperature where location = 'London'").all()
        val rec = new ListBuffer[Row]
        for (i <- 0 until rows.size)  rec += rows.get(i)
        val labels = rec.map("\"" + _.getTimestamp(0) + "\"").mkString("[", ",", "]")
        val minimums = rec.map(_.getString(1)).mkString("[", ",", "]")
        val means = rec.map(_.getString(2)).mkString("[", ",", "]")
        val maximums = rec.map(_.getString(3)).mkString("[", ",", "]")
        session.close
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html.format(labels, minimums, maximums, means)))
      }
    }

   val bindingFuture = Http().bindAndHandle(route, "localhost", 9080)

   println(s"Server online at http://localhost:9080/\nPress RETURN to stop...")
   StdIn.readLine()
   bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

 }
