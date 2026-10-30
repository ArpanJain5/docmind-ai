import { ApiError, request, requestStream } from "./http";

// The whole reply at once (not used by the screen anymore)
export function sendMessage(message, conversationId) {
  // returns { conversationId, reply }
  return request("/chat", {
    method: "POST",
    body: { message, conversationId },
  });
}

// One server event looks like:  "event:token\ndata:{...}"
function parseEvent(raw) {
  let name = "message";
  const dataLines = [];

  for (const line of raw.split("\n")) {
    if (line.startsWith("event:")) {
      name = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).replace(/^ /, ""));
    }
  }

  if (dataLines.length === 0) return null;

  try {
    return { name, data: JSON.parse(dataLines.join("\n")) };
  } catch {
    return null;
  }
}

// The reply piece by piece. Calls onMeta(conversationId) first, then onToken(text) for each piece.
// Resolves when the server says "done". Throws if the stream fails or is stopped.
export async function streamMessage(message, conversationId, { onMeta, onToken }, signal) {
  const response = await requestStream("/chat/stream", {
    body: { message, conversationId },
    signal,
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let finished = false;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, "\n");

    // Events are separated by a blank line
    let boundary;
    while ((boundary = buffer.indexOf("\n\n")) !== -1) {
      const event = parseEvent(buffer.slice(0, boundary));
      buffer = buffer.slice(boundary + 2);
      if (!event) continue;

      if (event.name === "meta") {
        onMeta(event.data.conversationId);
      } else if (event.name === "token") {
        onToken(event.data.text);
      } else if (event.name === "done") {
        finished = true;
      } else if (event.name === "error") {
        throw new ApiError(502, event.data.message);
      }
    }
  }

  if (!finished) {
    throw new ApiError(0, "The connection closed before the reply was finished.");
  }
}

export async function fetchMessages(conversationId) {
  try {
    const data = await request(`/conversations/${conversationId}/messages`);
    return data.map((m) => ({ role: m.role, text: m.content }));
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null; // this conversation no longer exists
    }
    throw error;
  }
}

export function fetchConversations() {
  // returns [{ id, title, createdAt }, ...] for the logged-in user, newest first
  return request("/conversations");
}

export function renameConversation(id, title) {
  // returns the updated { id, title, createdAt }
  return request(`/conversations/${id}`, { method: "PATCH", body: { title } });
}

export function deleteConversation(id) {
  return request(`/conversations/${id}`, { method: "DELETE" });
}