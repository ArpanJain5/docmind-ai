import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const [message, setMessage] = useState("Loading...");

  useEffect(() => {
    fetch("http://localhost:8080/api/hello")
      .then((response) => response.text())
      .then((text) => setMessage(text))
      .catch((error) => setMessage("Could not reach backend: " + error.message));
  }, []);

  return (
    <div className="app">
      <h1>DocMind AI 🧠</h1>
      <p>{message}</p>
    </div>
  );
}

export default App;