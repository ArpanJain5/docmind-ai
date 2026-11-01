import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "../auth/AuthContext";

// Pages inside this route need a logged-in user. Everyone else goes to /login.
function ProtectedRoute() {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}

export default ProtectedRoute;