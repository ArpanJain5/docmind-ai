// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { AuthContext } from "../auth/AuthContext";
import ChatPage from "./ChatPage";
import * as chatApi from "../api/chatApi";
import * as documentsApi from "../api/documentsApi";

vi.mock("../api/chatApi", () => ({
  fetchConversations: vi.fn(),
  fetchMessages: vi.fn(),
  renameConversation: vi.fn(),
  deleteConversation: vi.fn(),
  streamMessage: vi.fn(),
}));

vi.mock("../api/documentsApi", async (importOriginal) => {
  const real = await importOriginal();
  return { ...real, fetchDocuments: vi.fn() };
});

const report = {
  id: 3,
  originalFilename: "report.pdf",
  sizeBytes: 100,
  status: "READY",
  pageCount: 2,
  charCount: 500,
  preview: "A short report",
  createdAt: "2026-11-03T10:00:00Z",
};
const scan = { ...report, id: 4, originalFilename: "scan.pdf", status: "NO_TEXT", pageCount: 1, charCount: 0 };

const chatAboutReport = {
  id: 5,
  title: "Summarize it",
  createdAt: "2026-11-04T10:00:00Z",
  documentId: 3,
  documentName: "report.pdf",
  documentTruncated: false,
};

beforeEach(() => {
  window.HTMLElement.prototype.scrollIntoView = vi.fn(); // jsdom has no scrolling
  localStorage.clear();
  chatApi.fetchConversations.mockResolvedValue([]);
  documentsApi.fetchDocuments.mockResolvedValue([report, scan]);
  // A reply that starts a new chat with id 5
  chatApi.streamMessage.mockImplementation(async (text, conversationId, handlers) => {
    handlers.onMeta(5);
    handlers.onToken("Answer");
  });
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

function renderChat() {
  const auth = { user: { displayName: "Arpan" }, logout() {} };
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter>
        <ChatPage />
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

function send(text) {
  fireEvent.change(screen.getByLabelText("Your message"), { target: { value: text } });
  fireEvent.click(screen.getByRole("button", { name: "Send" }));
}

describe("ChatPage and documents", () => {
  it("offers only readable documents when a new chat starts", async () => {
    renderChat();

    await screen.findByLabelText("Chat about a document");
    const options = screen.getAllByRole("option").map((option) => option.textContent);
    expect(options).toEqual(["No document (ordinary chat)", "report.pdf (2 pages)"]);
  });

  it("sends the chosen document with the first message only, and shows which document the chat is about", async () => {
    chatApi.fetchConversations.mockResolvedValueOnce([]).mockResolvedValue([chatAboutReport]);
    renderChat();

    fireEvent.change(await screen.findByLabelText("Chat about a document"), { target: { value: "3" } });
    send("Summarize it");

    await waitFor(() => expect(chatApi.streamMessage).toHaveBeenCalledTimes(1));
    const first = chatApi.streamMessage.mock.calls[0];
    expect(first[1]).toBeNull(); // a new chat
    expect(first[4]).toBe(3); // about document 3

    expect(await screen.findByText("report.pdf")).toBeTruthy(); // the banner
    expect(screen.queryByLabelText("Chat about a document")).toBeNull(); // the choice is gone

    // The second message goes into chat 5 and does not repeat the document
    await screen.findByRole("button", { name: "Send" });
    send("And the dates?");
    await waitFor(() => expect(chatApi.streamMessage).toHaveBeenCalledTimes(2));
    const second = chatApi.streamMessage.mock.calls[1];
    expect(second[1]).toBe(5);
    expect(second[4]).toBeNull();
  });

  it("an ordinary chat sends no document", async () => {
    renderChat();
    await screen.findByLabelText("Chat about a document");

    send("Hello there");

    await waitFor(() => expect(chatApi.streamMessage).toHaveBeenCalledTimes(1));
    expect(chatApi.streamMessage.mock.calls[0][4]).toBeNull();
  });

  it("shows the document and the cut-off warning for an open chat, without the choice", async () => {
    localStorage.setItem("docmind.conversationId", "5");
    chatApi.fetchMessages.mockResolvedValue([{ role: "user", text: "Summarize it" }]);
    chatApi.fetchConversations.mockResolvedValue([{ ...chatAboutReport, documentTruncated: true }]);
    renderChat();

    expect(await screen.findByText(/Chatting about/)).toBeTruthy();
    expect(screen.getByText(/reads only the first part/)).toBeTruthy();
    expect(screen.queryByLabelText("Chat about a document")).toBeNull();
  });

  it("shows no banner for an ordinary open chat", async () => {
    localStorage.setItem("docmind.conversationId", "6");
    chatApi.fetchMessages.mockResolvedValue([{ role: "user", text: "Hi" }]);
    chatApi.fetchConversations.mockResolvedValue([
      { id: 6, title: "Hi", createdAt: "2026-11-04T10:00:00Z", documentId: null, documentName: null, documentTruncated: false },
    ]);
    renderChat();

    await screen.findByText("Hi", { selector: ".chat-item" });
    expect(screen.queryByText(/Chatting about/)).toBeNull();
  });

  it("does not offer the choice in an existing chat, even an empty one", async () => {
    localStorage.setItem("docmind.conversationId", "7");
    chatApi.fetchMessages.mockResolvedValue([]); // an open chat without messages
    chatApi.fetchConversations.mockResolvedValue([
      { id: 7, title: "Empty", createdAt: "2026-11-04T10:00:00Z", documentId: null, documentName: null, documentTruncated: false },
    ]);
    renderChat();

    await screen.findByText("Empty", { selector: ".chat-item" });
    await waitFor(() => expect(documentsApi.fetchDocuments).toHaveBeenCalled());
    await new Promise((resolve) => setTimeout(resolve, 0)); // let the documents arrive
    expect(screen.queryByLabelText("Chat about a document")).toBeNull();
  });
});