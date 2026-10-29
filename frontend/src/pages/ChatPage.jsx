import { useEffect, useRef, useState } from "react";
import {
  deleteConversation,
  fetchConversations,
  fetchMessages,
  renameConversation,
  sendMessage,
} from "../api/chatApi";
import { useAuth } from "../auth/AuthContext";
import "../App.css";

const STORAGE_KEY = "docmind.conversationId";
const WELCOME = { role: "assistant", text: "Hi! I'm DocMind. Ask me anything." };

function ChatPage() {
  const { user, logout } = useAuth();
  const [messages, setMessages] = useState([WELCOME]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [opening, setOpening] = useState(false);
  const [conversations, setConversations] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [editingTitle, setEditingTitle] = useState("");
  const [conversationId, setConversationId] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? Number(saved) : null;
  });
  const bottomRef = useRef(null);

  const busy = loading || opening;

  // First load: the sidebar list, and the chat that was open last time
  useEffect(() => {
    fetchConversations()
      .then(setConversations)
      .catch(() => {
        // The sidebar is a convenience. If it fails, the chat still works.
      });

    if (!conversationId) return;

    fetchMessages(conversationId)
      .then((history) => {
        if (history === null) {
          // Gone, or not ours: forget it
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

  async function refreshConversations() {
    try {
      setConversations(await fetchConversations());
    } catch {
      // ignore: see the comment above
    }
  }

  function showError(text) {
    setMessages((prev) => [...prev, { role: "assistant", text, error: true }]);
  }

  async function handleSend() {
    const text = input.trim();
    if (!text || busy) return;

    const isNewChat = conversationId === null;

    setMessages((prev) => [...prev, { role: "user", text }]);
    setInput("");
    setLoading(true);

    try {
      const result = await sendMessage(text, conversationId);
      setConversationId(result.conversationId);
      localStorage.setItem(STORAGE_KEY, String(result.conversationId));
      setMessages((prev) => [...prev, { role: "assistant", text: result.reply }]);
      if (isNewChat) {
        refreshConversations(); // the new chat appears in the sidebar
      }
    } catch (error) {
      // The server undoes a failed message, so we undo it on screen too
      // and put the text back in the box, ready to send again.
      setMessages((prev) => [
        ...prev.slice(0, -1),
        {
          role: "assistant",
          text:
            "Sorry, something went wrong: " +
            error.message +
            " Your message is back in the box. Press Send to try again.",
          error: true,
        },
      ]);
      setInput(text);
    } finally {
      setLoading(false);
    }
  }

  async function handleSelect(id) {
    if (busy || id === conversationId) return;

    setOpening(true);
    try {
      const history = await fetchMessages(id);
      if (history === null) {
        await refreshConversations(); // it vanished: update the list
        return;
      }
      setConversationId(id);
      localStorage.setItem(STORAGE_KEY, String(id));
      setMessages(history.length > 0 ? history : [WELCOME]);
    } catch (error) {
      showError("Could not open that chat: " + error.message);
    } finally {
      setOpening(false);
    }
  }

  // ---- rename: edit the title right in the sidebar ----
  function startRename(chat) {
    if (busy) return;
    setEditingId(chat.id);
    setEditingTitle(chat.title);
  }

  function cancelRename() {
    setEditingId(null);
  }

  async function saveRename(chat) {
    const clean = editingTitle.trim().slice(0, 120);
    setEditingId(null);
    if (!clean || clean === chat.title) return;

    try {
      const updated = await renameConversation(chat.id, clean);
      setConversations((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
    } catch (error) {
      showError("Could not rename the chat: " + error.message);
    }
  }

  async function handleDelete(chat) {
    if (busy) return;
    if (!window.confirm(`Delete "${chat.title}"? This cannot be undone.`)) return;

    try {
      await deleteConversation(chat.id);
      if (chat.id === conversationId) {
        localStorage.removeItem(STORAGE_KEY);
        setConversationId(null);
        setMessages([WELCOME]);
      }
      await refreshConversations();
    } catch (error) {
      showError("Could not delete the chat: " + error.message);
    }
  }

  function handleNewChat() {
    if (busy) return;
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
    <div className="shell">
      <aside className="sidebar">
        <h2>My chats</h2>
        {conversations.length === 0 && (
          <p className="empty">No chats yet. Say hello!</p>
        )}
        <ul>
          {conversations.map((c) => (
            <li key={c.id} className="chat-row">
              {editingId === c.id ? (
                <input
                  className="rename-input"
                  value={editingTitle}
                  onChange={(e) => setEditingTitle(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") saveRename(c);
                    if (e.key === "Escape") cancelRename();
                  }}
                  onBlur={cancelRename}
                  onFocus={(e) => e.target.select()}
                  maxLength={120}
                  aria-label="Chat name"
                  autoFocus
                />
              ) : (
                <>
                  <button
                    className={`chat-item${c.id === conversationId ? " active" : ""}`}
                    onClick={() => handleSelect(c.id)}
                    disabled={busy}
                    title={c.title}
                  >
                    {c.title}
                  </button>
                  <button
                    className="icon-btn"
                    onClick={() => startRename(c)}
                    disabled={busy}
                    title="Rename"
                    aria-label={`Rename ${c.title}`}
                  >
                    ✏️
                  </button>
                  <button
                    className="icon-btn"
                    onClick={() => handleDelete(c)}
                    disabled={busy}
                    title="Delete"
                    aria-label={`Delete ${c.title}`}
                  >
                    🗑️
                  </button>
                </>
              )}
            </li>
          ))}
        </ul>
      </aside>

      <div className="app">
        <div className="header">
          <h1>DocMind AI 🧠</h1>
          <div className="header-actions">
            <span className="user-name">{user.displayName}</span>
            <button className="new-chat" onClick={handleNewChat} disabled={busy}>
              New chat
            </button>
            <button className="new-chat" onClick={logout} disabled={busy}>
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
          <button onClick={handleSend} disabled={busy || !input.trim()}>
            Send
          </button>
        </div>
      </div>
    </div>
  );
}

export default ChatPage;