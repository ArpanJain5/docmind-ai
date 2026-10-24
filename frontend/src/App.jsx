import { useAuth } from "./auth/AuthContext";
import AuthPage from "./pages/AuthPage";
import ChatPage from "./pages/ChatPage";

function App() {
  const { user, checking } = useAuth();

  if (checking) {
    return <p className="status">Checking your session...</p>;
  }

  return user ? <ChatPage /> : <AuthPage />;
}

export default App;