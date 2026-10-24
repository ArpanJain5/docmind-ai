import { ApiError, request } from "./http";

export function sendMessage(message, conversationId) {
  // returns { conversationId, reply }
  return request("/chat", {
    method: "POST",
    body: { message, conversationId },
  });
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