import { ApiError, request } from "./http";

export async function register(email, password, displayName) {
  try {
    return await request("/auth/register", {
      method: "POST",
      body: { email, password, displayName },
      skipAuth: true,
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 409) {
      throw new ApiError(409, "That email is already registered.");
    }
    if (error instanceof ApiError && error.status === 400) {
      throw new ApiError(
        400,
        "Enter a valid email, your name, and a password of 8 to 72 characters."
      );
    }
    throw error;
  }
}

export async function login(email, password) {
  try {
    return await request("/auth/login", {
      method: "POST",
      body: { email, password },
      skipAuth: true,
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      throw new ApiError(401, "Invalid email or password.");
    }
    throw error;
  }
}

export function fetchMe() {
  return request("/auth/me");
}