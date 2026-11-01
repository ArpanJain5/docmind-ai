// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { AuthContext } from "../auth/AuthContext";
import AuthPage from "../pages/AuthPage";
import NotFoundPage from "../pages/NotFoundPage";
import ProtectedRoute from "./ProtectedRoute";
import PublicOnlyRoute from "./PublicOnlyRoute";

afterEach(cleanup);

// The same route layout as the real app, but with a plain stand-in for the chat screen
function renderAt(path, user = null) {
  const auth = {
    user,
    checking: false,
    serverDown: false,
    login() {},
    register() {},
    logout() {},
    retrySession() {},
  };

  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<AuthPage mode="login" />} />
            <Route path="/register" element={<AuthPage mode="register" />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route path="/chat" element={<p>Chat screen</p>} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

const loggedInUser = { id: 1, email: "a@example.com", displayName: "Arpan" };

describe("route guards", () => {
  it("sends a logged-out visitor from /chat to the login page", () => {
    renderAt("/chat");

    expect(screen.getByText("Log in to continue")).toBeTruthy();
    expect(screen.queryByText("Chat screen")).toBeNull();
  });

  it("lets a logged-in user open /chat", () => {
    renderAt("/chat", loggedInUser);

    expect(screen.getByText("Chat screen")).toBeTruthy();
  });

  it("sends a logged-in user away from /login to the chat", () => {
    renderAt("/login", loggedInUser);

    expect(screen.getByText("Chat screen")).toBeTruthy();
    expect(screen.queryByText("Log in to continue")).toBeNull();
  });
});

describe("pages", () => {
  it("login and register pages link to each other", () => {
    renderAt("/login");
    expect(screen.getByRole("link", { name: "Create an account" }).getAttribute("href")).toBe(
      "/register"
    );
    cleanup();

    renderAt("/register");
    expect(screen.getByText("Create your account")).toBeTruthy();
    expect(screen.getByRole("link", { name: "Log in" }).getAttribute("href")).toBe("/login");
  });

  it("shows a not-found page for an unknown address", () => {
    renderAt("/does-not-exist");

    expect(screen.getByText("Page not found")).toBeTruthy();
    expect(screen.getByRole("link", { name: "Go to my chats" }).getAttribute("href")).toBe(
      "/chat"
    );
  });
});