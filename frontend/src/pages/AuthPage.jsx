import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import "./AuthPage.css";

function AuthPage() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState("login");
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const isRegister = mode === "register";

  function switchMode() {
    setMode(isRegister ? "login" : "register");
    setError("");
  }

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
      // On success the app switches to the chat screen by itself
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

        {error && <div className="error-box">{error}</div>}

        <button className="submit" type="submit" disabled={submitting}>
          {submitting ? "Please wait..." : isRegister ? "Create account" : "Log in"}
        </button>
      </form>

      <p className="switch">
        {isRegister ? "Already have an account? " : "New here? "}
        <button type="button" className="link-button" onClick={switchMode}>
          {isRegister ? "Log in" : "Create an account"}
        </button>
      </p>
    </div>
  );
}

export default AuthPage;