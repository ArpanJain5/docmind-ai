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
  // True when we could not even ask the server who we are (it is down or unreachable)
  const [serverDown, setServerDown] = useState(false);

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

  // Asks the server "who am I?" for the saved token
  const checkSession = useCallback((isCancelled = () => false) => {
    return authApi
      .fetchMe()
      .then((me) => {
        if (!isCancelled()) setUser(me);
      })
      .catch((error) => {
        // A 401 already logged us out inside request().
        // If the server itself is unreachable or broken, say so instead of showing the login page.
        if (!isCancelled() && (error.status === 0 || error.status >= 500)) {
          setServerDown(true);
        }
      })
      .finally(() => {
        if (!isCancelled()) setChecking(false);
      });
  }, []);

  // On page load: if we have a saved token, ask "who am I?"
  useEffect(() => {
    if (!getToken()) return;

    let cancelled = false;
    checkSession(() => cancelled);

    return () => {
      cancelled = true;
    };
  }, [checkSession]);

  function retrySession() {
    setServerDown(false);
    setChecking(true);
    checkSession();
  }

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
    () => ({ user, checking, serverDown, login, register, logout, retrySession }),
    // retrySession only uses stable functions, so it is safe to leave out of the list
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [user, checking, serverDown, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}