const API_BASE = "http://localhost:8080/api";

export async function sendMessage(message) {
  const response = await fetch(`${API_BASE}/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message }),
  });

  if (!response.ok) {
    throw new Error("Server returned " + response.status);
  }

  const data = await response.json();
  return data.reply;
}