import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, setUnauthorizedHandler } from "./http";
import { fetchMessages, parseEvent, streamMessage } from "./chatApi";
import { jsonResponse, streamResponse, stubStorage } from "../test/helpers";

describe("parseEvent()", () => {
  it("reads the event name and the JSON data", () => {
    const event = parseEvent('event:token\ndata:{"text":"Hel"}');
    expect(event).toEqual({ name: "token", data: { text: "Hel" } });
  });

  it("accepts a space after the colon too", () => {
    const event = parseEvent('event: meta\ndata: {"conversationId":7}');
    expect(event).toEqual({ name: "meta", data: { conversationId: 7 } });
  });

  it("keeps newlines that are inside the JSON text", () => {
    const event = parseEvent('event:token\ndata:{"text":"a\\nb"}');
    expect(event.data.text).toBe("a\nb");
  });

  it("ignores events without data and data that is not JSON", () => {
    expect(parseEvent("event:done")).toBeNull();
    expect(parseEvent("event:token\ndata:not json")).toBeNull();
  });
});

describe("streamMessage()", () => {
  beforeEach(() => {
    stubStorage({ "docmind.token": "abc123" });
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    setUnauthorizedHandler(null);
    vi.unstubAllGlobals();
  });

  it("delivers the chat id first and then every piece, even when events are split across chunks", async () => {
    // The second event is cut in the middle, like a real network
    fetch.mockResolvedValue(
      streamResponse([
        'event:meta\ndata:{"conversationId":42}\n\nevent:tok',
        'en\ndata:{"text":"Hel"}\n\nevent:token\ndata:{"text":"lo"}\n\n',
        "event:done\ndata:{}\n\n",
      ])
    );

    const onMeta = vi.fn();
    const pieces = [];
    await streamMessage("hi", null, { onMeta, onToken: (p) => pieces.push(p) });

    expect(onMeta).toHaveBeenCalledWith(42);
    expect(pieces).toEqual(["Hel", "lo"]);
  });

  it("sends the message, the conversation id, and the token", async () => {
    fetch.mockResolvedValue(streamResponse(["event:done\ndata:{}\n\n"]));

    await streamMessage("hello", 9, { onMeta() {}, onToken() {} });

    const [url, options] = fetch.mock.calls[0];
    expect(url).toContain("/chat/stream");
    expect(JSON.parse(options.body)).toEqual({ message: "hello", conversationId: 9 });
    expect(options.headers.Authorization).toBe("Bearer abc123");
  });

  it("throws when the server sends an error event", async () => {
    fetch.mockResolvedValue(
      streamResponse([
        'event:token\ndata:{"text":"partial"}\n\n',
        'event:error\ndata:{"message":"The AI service is unavailable right now."}\n\n',
      ])
    );

    await expect(
      streamMessage("hi", null, { onMeta() {}, onToken() {} })
    ).rejects.toMatchObject({ status: 502, message: "The AI service is unavailable right now." });
  });

  it("throws when the connection closes before the done event", async () => {
    fetch.mockResolvedValue(streamResponse(['event:token\ndata:{"text":"half"}\n\n']));

    await expect(
      streamMessage("hi", null, { onMeta() {}, onToken() {} })
    ).rejects.toMatchObject({ message: expect.stringContaining("closed before the reply was finished") });
  });

  it("throws the HTTP status when the stream cannot even start (for example a 404)", async () => {
    fetch.mockResolvedValue(new Response(null, { status: 404 }));

    await expect(
      streamMessage("hi", 999, { onMeta() {}, onToken() {} })
    ).rejects.toMatchObject({ status: 404 });
  });
});

describe("fetchMessages()", () => {
  beforeEach(() => {
    stubStorage({ "docmind.token": "abc123" });
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("maps the server fields to what the screen needs", async () => {
    fetch.mockResolvedValue(
      jsonResponse([{ id: 1, role: "user", content: "hi", createdAt: "x" }])
    );

    await expect(fetchMessages(5)).resolves.toEqual([{ role: "user", text: "hi" }]);
  });

  it("returns null when the conversation does not exist (or is not yours)", async () => {
    fetch.mockResolvedValue(new Response(null, { status: 404 }));

    await expect(fetchMessages(5)).resolves.toBeNull();
  });

  it("rethrows other errors", async () => {
    fetch.mockResolvedValue(new Response(null, { status: 500 }));

    await expect(fetchMessages(5)).rejects.toBeInstanceOf(ApiError);
  });
});