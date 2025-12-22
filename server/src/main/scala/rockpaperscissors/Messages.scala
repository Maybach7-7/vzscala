package rockpaperscissors

sealed trait ClientMessage
case class Join(roomId: String, playerId: String) extends ClientMessage
case class Move(roomId: String, playerId: String, choice: String) extends ClientMessage

sealed trait ServerMessage
case class Ready(roomId: String) extends ServerMessage
case class Result(
    roomId: String, 
    winner: Option[String], 
    player1Score: Int, 
    player2Score: Int,
    player1Choice: String,
    player2Choice: String
) extends ServerMessage
case class Error(message: String) extends ServerMessage

sealed trait WebSocketMessage
case class OutgoingMessage(json: String) extends WebSocketMessage
case object ConnectionClosed extends WebSocketMessage

type WebSocketSender = String => Unit