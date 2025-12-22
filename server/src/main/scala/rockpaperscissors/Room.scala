package rockpaperscissors

import akka.actor.typed.ActorRef
import scala.collection.mutable

case class Player(id: String, sender: WebSocketSender)

case class Room(
    id: String,
    players: mutable.Map[String, Player] = mutable.Map.empty
) {
  var score: (Int, Int) = (0, 0)

  def addPlayer(player: Player): Boolean = {
    if (players.size < 2 && !players.contains(player.id)) {
      players += (player.id -> player)
      true
    } else {
      false
    }
  }

  def removePlayer(playerId: String): Unit = {
    players -= playerId
    score = (0, 0)
  }

  def getPlayerIds: List[String] = players.keys.toList
  def getOtherPlayer(currentPlayerId: String): Option[Player] = {
    players.find(_._1 != currentPlayerId).map(_._2)
  }

  def isReady: Boolean = players.size == 2
}