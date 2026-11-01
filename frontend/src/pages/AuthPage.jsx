import { useState } from "react";
import { Link } from "react-router";
import { useAuth } from "../auth/AuthContext";
import { usePageTitle } from "../hooks/usePageTitle";
import "./AuthPage.css";

// mode is "login" or "register" and comes from the URL (/login or /register)
function AuthPage({ mode }) {
  const { login, register } = useAuth();
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const isRegister = mode === "register";
  usePageTitle(isRegister ? "Create account" : "Log in");

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return;

    setError("");
    setSubmitting(true);
    try {
      if (isRegister) {
        await register(email.trim(), password, displayName.trim());
      } else {
        await login(email.trim(), password);
      }
      // On success the router sends the user on to the chat by itself
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  }

  return (
    <div className="auth">
      <h1>DocMind AI 🧠</h1>
      <p className="subtitle">
        {isRegister ? "Create your account" : "Log in to continue"}
      </p>

      <form onSubmit={handleSubmit}>
        {isRegister && (
          <label>
            Name
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              autoComplete="name"
              maxLength={80}
              required
            />
          </label>
        )}

        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            maxLength={254}
            required
          />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={isRegister ? "new-password" : "current-password"}
            minLength={isRegister ? 8 : undefined}
            maxLength={72}
            required
          />
        </label>

        {error && (
          <div className="error-box" role="alert">
            {error}
          </div>
        )}

        <button className="submit" type="submit" disabled={submitting}>
          {submitting ? "Please wait..." : isRegister ? "Create account" : "Log in"}
        </button>
      </form>

      <p className="switch">
        {isRegister ? "Already have an account? " : "New here? "}
        <Link className="link-button" to={isRegister ? "/login" : "/register"}>
          {isRegister ? "Log in" : "Create an account"}
        </Link>
      </p>
    </div>
  );
}

export default AuthPage;