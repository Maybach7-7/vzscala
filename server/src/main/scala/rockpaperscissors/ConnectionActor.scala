package rockpaperscissors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.scaladsl.SourceQueueWithComplete
import spray.json._

object ConnectionActor {
  sealed trait Command
  case class SendMessage(json: String) extends Command
  case class ProcessMessage(text: String, gameServer: ActorRef[GameServer.Command]) extends Command

  def apply(
    gameServer: ActorRef[GameServer.Command],
    outboundSink: SourceQueueWithComplete[String]
  ): Behavior[Command] =
    Behaviors.setup(context => new ConnectionActor(context, gameServer, outboundSink))
}

class ConnectionActor(
  context: ActorContext[ConnectionActor.Command],
  gameServer: ActorRef[GameServer.Command],
  outboundSink: akka.stream.scaladsl.SourceQueueWithComplete[String]
) extends AbstractBehavior[ConnectionActor.Command](context) {

  override def onMessage(msg: ConnectionActor.Command): Behavior[ConnectionActor.Command] = {
    import JsonProtocol._

    msg match {
      case ConnectionActor.SendMessage(json) =>
        outboundSink.offer(json)
        this

      case ConnectionActor.ProcessMessage(text, _) =>
        try {
          import JsonProtocol._
          val clientMessage = text.parseJson.convertTo[ClientMessage]
          clientMessage match {
            case join: Join =>
              val sender: WebSocketSender = outboundSink.offer(_)
              gameServer ! GameServer.Connect(join.playerId, join.roomId, sender)
            case move: Move =>
              gameServer ! GameServer.MakeMove(move.playerId, move.roomId, move.choice)
          }
        } catch {
          case e: Exception =>
            println(s"Error parsing message: $text, Error: $e") // чисто для отладки
            outboundSink.offer("""{"type":"error","message":"Invalid message format"}""")
        }
        this
    }
  }
}