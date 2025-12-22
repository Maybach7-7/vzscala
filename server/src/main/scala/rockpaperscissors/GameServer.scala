package rockpaperscissors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import spray.json._
import JsonProtocol._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection.mutable
import java.util.UUID

object GameServer {
  sealed trait Command
  case class Connect(playerId: String, room: String, sender: WebSocketSender) extends Command
  case class Disconnect(playerId: String, room: String) extends Command
  case class MakeMove(playerId: String, room: String, move: String) extends Command
  case class SendMessageToRoom(roomId: String, message: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new GameServer(context))

  implicit val timeout: Timeout = 5.seconds
}

class GameServer(context: ActorContext[GameServer.Command])
  extends AbstractBehavior[GameServer.Command](context) {

  import GameServer._

  private val rooms: mutable.Map[String, Room] = mutable.Map.empty
  private val moves: mutable.Map[(String, String), String] = mutable.Map.empty 

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Connect(playerId, roomId, connection) =>
        handleConnect(playerId, roomId, connection)
        this

      case Disconnect(playerId, roomId) =>
        handleDisconnect(playerId, roomId)
        this

      case MakeMove(playerId, roomId, move) =>
        handleMove(playerId, roomId, move)
        this

      case SendMessageToRoom(roomId, message) =>
        sendToRoom(roomId, message)
        this
    }
  }

  private def handleConnect(playerId: String, roomId: String, sender: WebSocketSender): Unit = {
    val room = rooms.getOrElseUpdate(roomId, Room(roomId))

    if (room.addPlayer(Player(playerId, sender))) {
      if (room.isReady) {
        val readyMsg = (Ready(roomId): ServerMessage).toJson.compactPrint

        room.players.values.foreach { player =>
          player.sender(readyMsg)
        }
      }
    } else {
      val errorMsg = (Error("Room is full"): ServerMessage).toJson.compactPrint
      sender(errorMsg)
    }
  }

  private def handleDisconnect(playerId: String, roomId: String): Unit = {
    rooms.get(roomId) match {
      case Some(room) =>
        room.removePlayer(playerId)

        val msg = (Error(s"Player $playerId disconnected"): ServerMessage).toJson.compactPrint
        room.players.values.foreach { player =>
          player.sender(msg)
        }

        if (room.players.isEmpty) {
          rooms.remove(roomId)
        }

        moves.filterKeys(_._1 == roomId).keys.foreach(moves.remove)

      case None =>
        context.log.warn(s"Attempted to disconnect player $playerId from non-existent room $roomId")
    }
  }

  private def handleMove(playerId: String, roomId: String, move: String): Unit = {
    if (!GameLogic.ValidMoves.contains(move)) {
      
      val errorMsg = (Error("Invalid move"): ServerMessage).toJson.compactPrint
      rooms.get(roomId).flatMap(_.players.get(playerId)).foreach { player =>
        player.sender(errorMsg)
      }
      return
    }

    moves((roomId, playerId)) = move

    rooms.get(roomId) match {
      case Some(room) if room.isReady =>
        val playerIds = room.getPlayerIds
        if (playerIds.forall(id => moves.contains((roomId, id)))) {
          val move1 = moves((roomId, playerIds(0)))
          val move2 = moves((roomId, playerIds(1)))

          val winnerOpt = GameLogic.determineWinner(move1, move2)

          var newScore = room.score
          winnerOpt match {
            case Some(1) => newScore = (room.score._1 + 1, room.score._2) 
            case Some(2) => newScore = (room.score._1, room.score._2 + 1) 
            case None => newScore = room.score 
          }

          room.score = newScore

          val resultMsg = Result(
            roomId = roomId,
            winner = winnerOpt.map(i => playerIds(i - 1)), // Convert 1/2 to player id
            player1Score = newScore._1,
            player2Score = newScore._2,
            player1Choice = move1,
            player2Choice = move2
          )

          val jsonResult = (resultMsg: ServerMessage).toJson.compactPrint

          room.players.values.foreach { player =>
            player.sender(jsonResult)
          }

          playerIds.foreach(id => moves.remove((roomId, id)))
        }

      case _ =>
        context.log.warn(s"Player $playerId attempted to move in invalid room $roomId")
    }
  }

  private def sendToRoom(roomId: String, message: String): Unit = {
    rooms.get(roomId).foreach { room =>
      room.players.values.foreach { player =>
        player.sender(message)
      }
    }
  }
}