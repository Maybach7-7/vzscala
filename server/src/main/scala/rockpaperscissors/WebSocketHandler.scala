package rockpaperscissors

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import spray.json._
import JsonProtocol._
import scala.concurrent.ExecutionContext

object WebSocketHandler {
  def apply(
    gameServer: ActorRef[GameServer.Command]
  )(implicit ec: ExecutionContext, system: ActorSystem[_]): Flow[Message, Message, Any] = {

    val (outboundSink, outboundSource) =
      Source.queue[String](128, OverflowStrategy.dropHead)
        .toMat(BroadcastHub.sink(bufferSize = 128))(Keep.both)
        .run()

    val connectionActor = system.systemActorOf(
      ConnectionActor(gameServer, outboundSink),
      s"conn-${java.util.UUID.randomUUID().toString.take(10)}"
    )

    Flow.fromSinkAndSource(
      Sink.foreach[Message] {
        case TextMessage.Strict(text) =>
          connectionActor ! ConnectionActor.ProcessMessage(text, gameServer)
        case _ =>
      },
      outboundSource.map(TextMessage(_))
    )
  }
}