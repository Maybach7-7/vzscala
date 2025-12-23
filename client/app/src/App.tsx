import React, { useState, useEffect } from "react";
import "./App.css";

// Define TypeScript interfaces
interface GameState {
  roomId: string;
  connected: boolean;
  ready: boolean;
  playerChoice: string | null;
  opponentChoice: string | null;
  playerScore: number;
  opponentScore: number;
  result: string | null;
  error: string | null;
}

interface ServerMessage {
  type: string;
  [key: string]: any;
}

const App: React.FC = () => {
  const [state, setState] = useState<GameState>({
    roomId: "",
    connected: false,
    ready: false,
    playerChoice: null,
    opponentChoice: null,
    playerScore: 0,
    opponentScore: 0,
    result: null,
    error: null,
  });

  const [ws, setWs] = useState<WebSocket | null>(null);
  const [playerId] = useState<string>(
    "player-" + Math.random().toString(36).substr(2, 9)
  );

  // Connect to WebSocket
  const connectToRoom = () => {
    if (!state.roomId.trim()) {
      setState((prev) => ({ ...prev, error: "Please enter a room ID" }));
      return;
    }

    // Close existing connection if any
    if (ws) {
      ws.close();
    }

    // Use environment variable for WebSocket URL or default
    const wsUrl = process.env.WS_URL || "ws://localhost:8080/ws";
    const newWs = new WebSocket(wsUrl);

    newWs.onopen = () => {
      console.log("Connected to WebSocket");
      setState((prev) => ({ ...prev, connected: true, error: null }));

      // Join the room
      newWs.send(
        JSON.stringify({
          type: "join",
          roomId: state.roomId,
          playerId: playerId,
        })
      );
    };

    newWs.onmessage = (event) => {
      const message: ServerMessage = JSON.parse(event.data);

      switch (message.type) {
        case "ready":
          setState((prev) => ({
            ...prev,
            ready: true,
            error: null,
            result: "Both players joined! Game starting...",
          }));
          break;

        case "result":
          // The server sends results with fixed positions (player1 and player2)
          // We need to determine which position we are based on the winner or other logic
          // For now, we'll assume the first join establishes player position
          setState((prev) => ({
            ...prev,
            playerChoice: message.player1Choice, // This will be our choice if we're player1
            opponentChoice: message.player2Choice, // This will be opponent's choice if we're player1
            playerScore: message.player1Score,
            opponentScore: message.player2Score,
            result: getResultText(
              message.winner,
              playerId,
              message.player1Choice,
              message.player2Choice
            ),
          }));
          break;

        case "error":
          setState((prev) => ({ ...prev, error: message.message }));
          break;

        default:
          console.log("Unknown message type:", message);
      }
    };

    newWs.onclose = () => {
      console.log("Disconnected from WebSocket");
      setState((prev) => ({
        ...prev,
        connected: false,
        ready: false,
        playerChoice: null,
        opponentChoice: null,
        result: null,
      }));
    };

    newWs.onerror = (error) => {
      console.error("WebSocket error:", error);
      setState((prev) => ({ ...prev, error: "Connection error occurred" }));
    };

    setWs(newWs);
  };

  // Send move to server
  const makeMove = (choice: string) => {
    if (!state.connected || !state.ready || !ws) {
      return;
    }

    ws.send(
      JSON.stringify({
        type: "move",
        roomId: state.roomId,
        playerId: playerId,
        choice: choice,
      })
    );

    setState((prev) => ({
      ...prev,
      playerChoice: choice,
      result: "Waiting for opponent...",
    }));
  };

  // Calculate result text
  const getResultText = (
    winner: string | null,
    playerId: string,
    p1Choice: string,
    p2Choice: string
  ): string => {
    if (winner === null) {
      return `Tie! You both chose ${p1Choice}`;
    } else if (winner === playerId) {
      return `You win! ${getWinningText(p1Choice, p2Choice)}`;
    } else {
      return `You lose! ${getLosingText(p1Choice, p2Choice)}`;
    }
  };

  const getWinningText = (p1Choice: string, p2Choice: string): string => {
    if (p1Choice === "rock" && p2Choice === "scissors") {
      return "Rock crushes scissors!";
    } else if (p1Choice === "paper" && p2Choice === "rock") {
      return "Paper covers rock!";
    } else if (p1Choice === "scissors" && p2Choice === "paper") {
      return "Scissors cut paper!";
    }
    return "";
  };

  const getLosingText = (p1Choice: string, p2Choice: string): string => {
    if (p1Choice === "rock" && p2Choice === "paper") {
      return "Paper covers rock!";
    } else if (p1Choice === "paper" && p2Choice === "scissors") {
      return "Scissors cut paper!";
    } else if (p1Choice === "scissors" && p2Choice === "rock") {
      return "Rock crushes scissors!";
    }
    return "";
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (ws) {
        ws.close();
      }
    };
  }, []);

  return (
    <div className="app">
      <header className="app-header">
        <h1>Rock Paper Scissors</h1>
      </header>

      <main className="app-main">
        {!state.connected ? (
          <div className="connect-section">
            <h2>Join a Game</h2>
            <div className="input-group">
              <label htmlFor="roomId">Room ID:</label>
              <input
                type="text"
                id="roomId"
                value={state.roomId}
                onChange={(e) =>
                  setState((prev) => ({ ...prev, roomId: e.target.value }))
                }
                placeholder="Enter room ID"
              />
            </div>
            <button onClick={connectToRoom}>Connect</button>

            {state.error && <div className="error">{state.error}</div>}
          </div>
        ) : (
          <div className="game-section">
            <div className="room-info">
              <p>Room: {state.roomId}</p>
              <p>
                Status:{" "}
                {state.connected
                  ? state.ready
                    ? "Ready"
                    : "Waiting for opponent..."
                  : "Disconnected"}
              </p>
            </div>

            {state.ready && (
              <>
                <div className="score-board">
                  <div className="player-score">You: {state.playerScore}</div>
                  <div className="opponent-score">
                    Opponent: {state.opponentScore}
                  </div>
                </div>

                <div className="moves-display">
                  <div className="player-move">
                    <h3>Your Move</h3>
                    <div className="choice">{state.playerChoice || "-"}</div>
                  </div>

                  <div className="opponent-move">
                    <h3>Opponent's Move</h3>
                    <div className="choice">{state.opponentChoice || "-"}</div>
                  </div>
                </div>

                <div className="controls">
                  <h3>Make Your Move</h3>
                  <div className="move-buttons">
                    <button
                      onClick={() => makeMove("rock")}
                      disabled={!state.ready}
                    >
                      🪨 Rock
                    </button>
                    <button
                      onClick={() => makeMove("paper")}
                      disabled={!state.ready}
                    >
                      📄 Paper
                    </button>
                    <button
                      onClick={() => makeMove("scissors")}
                      disabled={!state.ready}
                    >
                      ✂️ Scissors
                    </button>
                  </div>
                </div>

                {state.result && (
                  <div className="result">
                    <h3>Round Result</h3>
                    <p>{state.result}</p>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </main>
    </div>
  );
};

export default App;
