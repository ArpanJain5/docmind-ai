import { Navigate, Route, Routes } from "react-router";
import { useAuth } from "./auth/AuthContext";
import AuthPage from "./pages/AuthPage";
import ChatPage from "./pages/ChatPage";
import DocumentsPage from "./pages/DocumentsPage";
import NotFoundPage from "./pages/NotFoundPage";
import ProtectedRoute from "./routes/ProtectedRoute";
import PublicOnlyRoute from "./routes/PublicOnlyRoute";

function App() {
  const { checking, serverDown, retrySession } = useAuth();

  if (checking) {
    return (
      <p className="status" role="status">
        Checking your session...
      </p>
    );
  }

  if (serverDown) {
    return (
      <div className="status-page" role="alert">
        <h1>Cannot reach the server</h1>
        <p>
          You are still logged in. The backend is not answering right now. Check that it is
          running, then try again.
        </p>
        <button className="primary-btn" onClick={retrySession}>
          Try again
        </button>
      </div>
    );
  }

  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<AuthPage key="login" mode="login" />} />
        <Route path="/register" element={<AuthPage key="register" mode="register" />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/documents" element={<DocumentsPage />} />
      </Route>

      <Route path="/" element={<Navigate to="/chat" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default App;