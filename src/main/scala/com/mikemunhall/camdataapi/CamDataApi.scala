package com.mikemunhall.camdataapi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import akka.pattern.ask
import akka.stream.scaladsl.Framing

import scala.concurrent.Future
import com.mikemunhall.camdataapi.IoManager.{GetRecords, ImageRecord, ImageRecords, SaveRecord}

import scala.io.StdIn
import scala.concurrent.ExecutionContext.Implicits.global

object CamDataApi {

  implicit val imageRecordFormat = jsonFormat3(ImageRecord)
  implicit val imageRecordsFormat = jsonFormat1(ImageRecords)

  def main(args: Array[String]) {

    implicit val system = ActorSystem("CamDataApi")
    implicit val materializer = ActorMaterializer()
    implicit val timeout: Timeout = 5.seconds

    val ioManager = system.actorOf(IoManager.props, "ioManager")

    val route =
      path("current") {
        get {
          val records = (ioManager ? GetRecords).mapTo[ImageRecords]
          complete(records)
        } ~
        (post & entity(as[Multipart.FormData])) { fileData =>
          uploadedFile("imageFile") {
            case (metadata, file) =>
              val imageRecord = (ioManager ? SaveRecord(metadata, file)).mapTo[ImageRecord]
              complete(imageRecord)
          }
        }
      } ~
      path("saved") {
        get {
          complete(StatusCodes.NotImplemented, "TODO")
        } ~
        post {
          complete(StatusCodes.NotImplemented, "TODO")
        } ~
        delete {
          complete(StatusCodes.NotImplemented, "TODO")
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println("Server online at http://localhost:8080. Press RETURN to stop")
    StdIn.readLine()

    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}
