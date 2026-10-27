const API_BASE = "http://localhost:8080/api";
const TOKEN_KEY = "docmind.token";

// ---- token storage (all localStorage access lives here) ----
export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function saveToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

// ---- "your session expired" hook, set by AuthProvider ----
let onUnauthorized = null;

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

// ---- a clean error type that carries the HTTP status ----
export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function defaultMessage(status) {
  if (status === 400) return "Please check what you entered.";
  if (status === 401) return "Your session expired. Please log in again.";
  if (status === 404) return "Not found.";
  if (status === 409) return "That already exists.";
  if (status === 502 || status === 503 || status === 504) {
    return "The AI service is unavailable right now. Please try again in a moment.";
  }
  if (status >= 500) return "Something went wrong on the server.";
  return "Request failed (" + status + ").";
}

// ---- the one function every API call goes through ----
export async function request(path, { method = "GET", body, skipAuth = false } = {}) {
  const headers = {};
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const token = skipAuth ? null : getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, "Cannot reach the server. Is the backend running?");
  }

  // We sent a token and the server said no: it expired or was tampered with
  if (response.status === 401 && token && onUnauthorized) {
    onUnauthorized();
  }

  if (!response.ok) {
    throw new ApiError(response.status, defaultMessage(response.status));
  }

  return response.json();
}