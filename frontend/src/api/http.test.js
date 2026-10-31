import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, request, setUnauthorizedHandler } from "./http";
import { jsonResponse, stubStorage } from "../test/helpers";

describe("request()", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    setUnauthorizedHandler(null);
    vi.unstubAllGlobals();
  });

  it("sends the saved token as a Bearer header", async () => {
    stubStorage({ "docmind.token": "abc123" });
    fetch.mockResolvedValue(jsonResponse({ ok: true }));

    await request("/auth/me");

    const options = fetch.mock.calls[0][1];
    expect(options.headers.Authorization).toBe("Bearer abc123");
  });

  it("does not send the token when skipAuth is set", async () => {
    stubStorage({ "docmind.token": "abc123" });
    fetch.mockResolvedValue(jsonResponse({ ok: true }));

    await request("/auth/login", { method: "POST", body: { a: 1 }, skipAuth: true });

    const options = fetch.mock.calls[0][1];
    expect(options.headers.Authorization).toBeUndefined();
    expect(options.headers["Content-Type"]).toBe("application/json");
  });

  it("logs the user out when a request that carried a token gets 401", async () => {
    stubStorage({ "docmind.token": "expired" });
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetch.mockResolvedValue(new Response(null, { status: 401 }));

    await expect(request("/conversations")).rejects.toMatchObject({ status: 401 });
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it("does not trigger a logout for a 401 when no token was sent", async () => {
    stubStorage();
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetch.mockResolvedValue(new Response(null, { status: 401 }));

    await expect(request("/auth/login", { skipAuth: true })).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it("turns a 502 into a friendly AI-unavailable message", async () => {
    stubStorage();
    fetch.mockResolvedValue(new Response(null, { status: 502 }));

    await expect(request("/chat")).rejects.toMatchObject({
      status: 502,
      message: expect.stringContaining("AI service is unavailable"),
    });
  });

  it("explains when the server cannot be reached", async () => {
    stubStorage();
    fetch.mockRejectedValue(new TypeError("Failed to fetch"));

    await expect(request("/chat")).rejects.toMatchObject({
      status: 0,
      message: expect.stringContaining("Cannot reach the server"),
    });
  });

  it("returns null for a 204 response with no body", async () => {
    stubStorage();
    fetch.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(request("/conversations/1", { method: "DELETE" })).resolves.toBeNull();
  });
});