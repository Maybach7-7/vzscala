package rockpaperscissors

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import spray.json._
import scala.concurrent.{ExecutionContext, Future, Await}
import akka.actor.typed.scaladsl.AskPattern._
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object MainServer {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[GameServer.Command] = ActorSystem(GameServer(), "rps-system")
    implicit val materializer: Materializer = Materializer(system)
    implicit val executionContext: ExecutionContext = system.executionContext

    val gameServer: ActorRef[GameServer.Command] = system

    val route: Route = {
      path("ws") {
        get {
          handleWebSocketMessages(WebSocketHandler(gameServer))
        }
      } ~
      pathEndOrSingleSlash {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, """
            <html>
              <body>
                <h1>Rock Paper Scissors Server</h1>
                <p>WebSocket endpoint available at /ws</p>
              </body>
            </html>
          """))
        }
      }
    }

    val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(route)
    println("Server online at http://0.0.0.0:8080/")

    bindingFuture.foreach { binding =>
      println(s"Bound to ${binding.localAddress}")
    }

    Await.result(bindingFuture.flatMap(_.whenTerminated), scala.concurrent.duration.Duration.Inf)
    system.terminate()
  }
}