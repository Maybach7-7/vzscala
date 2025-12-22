package rockpaperscissors

import spray.json._

object JsonProtocol extends DefaultJsonProtocol {
  
  implicit val joinFormat: RootJsonFormat[Join] = jsonFormat2(Join.apply)
  implicit val moveFormat: RootJsonFormat[Move] = jsonFormat3(Move.apply)
  implicit val readyFormat: RootJsonFormat[Ready] = jsonFormat1(Ready.apply)
  implicit val errorFormat: RootJsonFormat[Error] = jsonFormat1(Error.apply)

  implicit object resultFormat extends RootJsonFormat[Result] {
    def write(result: Result): JsValue = JsObject(
      "roomId" -> JsString(result.roomId),
      "winner" -> (result.winner match {
        case Some(id) => JsString(id)
        case None => JsNull
      }),
      "player1Score" -> JsNumber(result.player1Score),
      "player2Score" -> JsNumber(result.player2Score),
      "player1Choice" -> JsString(result.player1Choice),
      "player2Choice" -> JsString(result.player2Choice)
    )

    def read(value: JsValue): Result = {
      val fields = value.asJsObject.fields
      Result(
        roomId = fields("roomId").convertTo[String],
        winner = fields.get("winner") match {
          case Some(JsString(id)) => Some(id)
          case Some(JsNull) | None => None
          case _ => None
        },
        player1Score = fields("player1Score").convertTo[Int],
        player2Score = fields("player2Score").convertTo[Int],
        player1Choice = fields("player1Choice").convertTo[String],
        player2Choice = fields("player2Choice").convertTo[String]
      )
    }
  }

  implicit object ServerMessageFormat extends RootJsonFormat[ServerMessage] {
    override def read(json: JsValue): ServerMessage = {
      val jsObj = json.asJsObject
      val fields = jsObj.fields
      fields.get("type") match {
        case Some(JsString("ready")) =>
          json.convertTo[Ready]
        case Some(JsString("result")) =>
          json.convertTo[Result]
        case Some(JsString("error")) =>
          json.convertTo[Error]
        case Some(JsString(unknownType)) =>
          throw DeserializationException(s"Unknown server message type: $unknownType")
        case None =>
          throw DeserializationException("Missing 'type' field in server message")
      }
    }

    override def write(obj: ServerMessage): JsValue = {
      val baseJson = obj match {
        case r: Ready => r.toJson
        case r: Result => r.toJson
        case e: Error => e.toJson
      }

      val typeField = "type" -> JsString(obj match {
        case _: Ready => "ready"
        case _: Result => "result"
        case _: Error => "error"
      })

      JsObject(baseJson.asJsObject.fields + typeField)
    }
  }

  implicit object ClientMessageFormat extends RootJsonFormat[ClientMessage] {
    override def read(json: JsValue): ClientMessage = {
      val jsObj = json.asJsObject
      val fields = jsObj.fields
      fields.get("type") match {
        case Some(JsString("join")) =>
          json.convertTo[Join]
        case Some(JsString("move")) =>
          json.convertTo[Move]
        case Some(JsString(unknownType)) =>
          throw DeserializationException(s"Unknown client message type: $unknownType")
        case None =>
          throw DeserializationException(s"Missing 'type' field in client message: ${json}")
      }
    }

    override def write(obj: ClientMessage): JsValue = {
      val baseJson = obj match {
        case j: Join => j.toJson
        case m: Move => m.toJson
      }

      val typeField = "type" -> JsString(obj match {
        case _: Join => "join"
        case _: Move => "move"
      })

      JsObject(baseJson.asJsObject.fields + typeField)
    }
  }
}