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

const App: React.FC = () => {};

export default App;
