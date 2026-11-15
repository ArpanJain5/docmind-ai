import "./DocumentChat.css";

// Shows which document the open chat is about. Shows nothing for an ordinary chat.
function DocumentBanner({ name, truncated }) {
  if (!name) return null;

  return (
    <div className="docchat-banner" role="status">
      <span>
        <span aria-hidden="true">📄 </span>
        Chatting about <strong>{name}</strong>
      </span>
      {truncated && (
        <span className="docchat-warning">DocMind reads only the first part of this document.</span>
      )}
    </div>
  );
}

export default DocumentBanner;