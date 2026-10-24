import { useEffect, useRef, useState } from "react";
import { fetchMessages, sendMessage } from "../api/chatApi";
import { useAuth } from "../auth/AuthContext";
import "../App.css";

const STORAGE_KEY = "docmind.conversationId";
const WELCOME = { role: "assistant", text: "Hi! I'm DocMind. Ask me anything." };

function ChatPage() {
  const { user, logout } = useAuth();
  const [messages, setMessages] = useState([WELCOME]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [conversationId, setConversationId] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? Number(saved) : null;
  });
  const bottomRef = useRef(null);

  // On first load: if we remember a conversation, fetch its history
  useEffect(() => {
    if (!conversationId) return;

    fetchMessages(conversationId)
      .then((history) => {
        if (history === null) {
          localStorage.removeItem(STORAGE_KEY);
          setConversationId(null);
        } else if (history.length > 0) {
          setMessages(history);
        }
      })
      .catch((error) => {
        setMessages([
          WELCOME,
          {
            role: "assistant",
            text: "Could not load your previous messages: " + error.message,
            error: true,
          },
        ]);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- run once on first load
  }, []);

  // Scroll to the newest message whenever something changes
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  async function handleSend() {
    const text = input.trim();
    if (!text || loading) return;

    setMessages((prev) => [...prev, { role: "user", text }]);
    setInput("");
    setLoading(true);

    try {
      const result = await sendMessage(text, conversationId);
      setConversationId(result.conversationId);
      localStorage.setItem(STORAGE_KEY, String(result.conversationId));
      setMessages((prev) => [...prev, { role: "assistant", text: result.reply }]);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          text: "Sorry, something went wrong: " + error.message,
          error: true,
        },
      ]);
    } finally {
      setLoading(false);
    }
  }

  function handleNewChat() {
    if (loading) return;
    localStorage.removeItem(STORAGE_KEY);
    setConversationId(null);
    setMessages([WELCOME]);
  }

  function handleKeyDown(event) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      handleSend();
    }
  }

  return (
    <div className="app">
      <div className="header">
        <h1>DocMind AI 🧠</h1>
        <div className="header-actions">
          <span className="user-name">{user.displayName}</span>
          <button className="new-chat" onClick={handleNewChat} disabled={loading}>
            New chat
          </button>
          <button className="new-chat" onClick={logout} disabled={loading}>
            Log out
          </button>
        </div>
      </div>

      <div className="messages">
        {messages.map((m, index) => (
          <div
            key={index}
            className={`bubble ${m.role}${m.error ? " error" : ""}`}
          >
            {m.text}
          </div>
        ))}
        {loading && (
          <div className="bubble assistant typing">DocMind is typing...</div>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="input-row">
        <textarea
          rows={2}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Type a message... (Enter to send, Shift+Enter for a new line)"
        />
        <button onClick={handleSend} disabled={loading || !input.trim()}>
          Send
        </button>
      </div>
    </div>
  );
}

export default ChatPage;