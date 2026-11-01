import { Link } from "react-router";
import { usePageTitle } from "../hooks/usePageTitle";

function NotFoundPage() {
  usePageTitle("Page not found");

  return (
    <div className="status-page">
      <h1>Page not found</h1>
      <p>That address does not exist.</p>
      <Link className="primary-btn" to="/chat">
        Go to my chats
      </Link>
    </div>
  );
}

export default NotFoundPage;