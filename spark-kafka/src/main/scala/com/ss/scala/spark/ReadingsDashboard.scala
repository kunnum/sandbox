package com.ss.scala.spark

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.datastax.driver.core.{Cluster, Row}

import scala.collection.mutable.ListBuffer
import scala.io.{Source, StdIn}

/**
  * Created by meeraj on 10/09/16.
  */
object ReadingsDashboard extends App {

  System.setProperty("spark.cassandra.connection.host", "127.0.0.1")

  implicit val system = ActorSystem("readings-data-dashboard")

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val html = Source.fromInputStream(getClass.getClassLoader.
    getResourceAsStream("dashboard.html")).mkString
  val cluster = Cluster.builder().
    addContactPoint("localhost").withPort(9042).build()

  val route =
    get {
      path("") {
        val session = cluster.connect()
        val rows = session.execute("select location, usage from demo.power_usage").all()
        val rec = new ListBuffer[Row]
        for (i <- 0 until rows.size)  rec += rows.get(i)
        val map = rec.map(r => r.getString(0) -> r.getString(1)).toMap
        session.close
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
          html.format(map("London"), map("Birmingham"), map("Glasgow"), map("Cardiff"), map("Belfast"))))
      }
    }

   val bindingFuture = Http().bindAndHandle(route, "localhost", 7080)

   println(s"Server online at http://localhost:9080/\nPress RETURN to stop...")
   StdIn.readLine()
   bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

 }
