package com.mikemunhall.camdataapi

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import spray.json.DefaultJsonProtocol._
import scala.concurrent.duration._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.mikemunhall.camdataapi.Auction.{Bid, Bids, GetBids}
import akka.pattern.ask
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.io.StdIn
import scala.concurrent.ExecutionContext.Implicits.global

object CamDataApi {

  implicit val bidFormat = jsonFormat2(Bid)
  implicit val bidsFormat = jsonFormat1(Bids)

  def main(args: Array[String]) {

    implicit val system = ActorSystem("CamDataApi")
    implicit val materializer = ActorMaterializer()
    //implicit val executionContext = system.dispatcher

    val auction = system.actorOf(Auction.props, "auction")

    val route =
      path("auction") {
        post {
          parameter("bid".as[Int], "user") { (bid, user) =>
            auction ! Bid(user, bid)
            complete(StatusCodes.Accepted, "bid placed")
          }
        } ~
        get {
          implicit val timeout: Timeout = 5.seconds
          val bids: Future[Bids] = (auction ? GetBids).mapTo[Bids]
          complete(bids)
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println("Server online at http://localhost:8080. Press RETURN to stop")
    StdIn.readLine()

    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}

object Auction {
  case class Bid(userId: String, bid: Int)
  case class Bids(bids: List[Bid])
  case object GetBids
  def props = Props[Auction]
}

class Auction extends Actor {

  //implicit val system = ActorSystem("CamDataApi")
  //implicit val executionContext = system.dispatcher

  val bids = ListBuffer[Bid]()

  def receive = {
    case GetBids => sender() ! Bids(bids.toList)
    case bid:Bid => bids += bid
    case o => println("other: " + o)
  }
}
