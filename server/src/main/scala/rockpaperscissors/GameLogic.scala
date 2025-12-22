package rockpaperscissors

object GameLogic {
  val ValidMoves = Set("rock", "paper", "scissors")
  
  def determineWinner(move1: String, move2: String): Option[Int] = {
    if (move1 == move2) {
      None
    } else if ((move1 == "rock" && move2 == "scissors") ||
               (move1 == "paper" && move2 == "rock") ||
               (move1 == "scissors" && move2 == "paper")) {
      Some(1) // Player 1 wins
    } else {
      Some(2) // Player 2 wins
    }
  }
}