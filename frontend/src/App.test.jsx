// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import App from "./App";
import { AuthProvider } from "./auth/AuthProvider";

describe("App when the server is down", () => {
  beforeEach(() => {
    localStorage.setItem("docmind.token", "saved-token");
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));
  });

  afterEach(() => {
    cleanup();
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("shows a server-down screen instead of the login page, and can try again", async () => {
    render(
      <MemoryRouter initialEntries={["/chat"]}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText("Cannot reach the server")).toBeTruthy();
    expect(screen.queryByText("Log in to continue")).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("Cannot reach the server")).toBeTruthy();
  });
});