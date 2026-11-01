import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "../auth/AuthContext";

// Login and register pages are for logged-out visitors. Logged-in users go to the chat
// (or back to the page they were trying to open before they were sent to log in).
function PublicOnlyRoute() {
  const { user } = useAuth();
  const location = useLocation();

  if (user) {
    const destination = location.state?.from?.pathname ?? "/chat";
    return <Navigate to={destination} replace />;
  }

  return <Outlet />;
}

export default PublicOnlyRoute;