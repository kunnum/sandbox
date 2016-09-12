package com.ss.scala.spark

import java.util.Properties

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scala.io.StdIn

/**
 * Created by meeraj on 10/09/16.
 */
object SensorDataReceiver extends App {

  val props = new Properties();
  props.put("bootstrap.servers", "localhost:9092");
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val route =
    post {
      path("") {
        entity(as[String]) { payload =>
          println(payload)
          val rec = new ProducerRecord[String, String]("weather", 0, java.util.UUID.randomUUID().toString, payload)
          val producer = new KafkaProducer[String, String](props);
          producer.send(rec)
          producer.close()
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, ""))
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, ""))
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
}
