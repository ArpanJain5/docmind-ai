import { useCallback, useEffect, useMemo, useState } from "react";
import * as authApi from "../api/authApi";
import {
  clearToken,
  getToken,
  saveToken,
  setUnauthorizedHandler,
} from "../api/http";
import { AuthContext } from "./AuthContext";

const CONVERSATION_KEY = "docmind.conversationId";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // If a token is saved, we must ask the server who it belongs to before showing anything
  const [checking, setChecking] = useState(() => Boolean(getToken()));

  const logout = useCallback(() => {
    clearToken();
    // Chats are not tied to users yet, so forget the open chat on logout
    localStorage.removeItem(CONVERSATION_KEY);
    setUser(null);
  }, []);

  // Let the request helper log us out when the server rejects our token
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  // On page load: if we have a saved token, ask "who am I?"
  useEffect(() => {
    if (!getToken()) return;

    let cancelled = false;
    authApi
      .fetchMe()
      .then((me) => {
        if (!cancelled) setUser(me);
      })
      .catch(() => {
        // A 401 already logged us out inside request(). Other errors
        // (like the backend being down) leave the saved token alone.
      })
      .finally(() => {
        if (!cancelled) setChecking(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email, password) => {
    const result = await authApi.login(email, password);
    saveToken(result.token);
    setUser(result.user);
  }, []);

  const register = useCallback(
    async (email, password, displayName) => {
      await authApi.register(email, password, displayName);
      await login(email, password);
    },
    [login]
  );

  const value = useMemo(
    () => ({ user, checking, login, register, logout }),
    [user, checking, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}