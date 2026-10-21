const API_BASE = "http://localhost:8080/api";

export async function sendMessage(message, conversationId) {
  const response = await fetch(`${API_BASE}/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message, conversationId }),
  });

  if (!response.ok) {
    throw new Error("Server returned " + response.status);
  }

  return response.json(); // { conversationId, reply }
}

export async function fetchMessages(conversationId) {
  const response = await fetch(
    `${API_BASE}/conversations/${conversationId}/messages`
  );

  if (response.status === 404) {
    return null; // this conversation no longer exists
  }
  if (!response.ok) {
    throw new Error("Server returned " + response.status);
  }

  const data = await response.json();
  return data.map((m) => ({ role: m.role, text: m.content }));
}