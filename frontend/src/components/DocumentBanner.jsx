import "./DocumentChat.css";

// Shows which document the open chat is about. Shows nothing for an ordinary chat.
function DocumentBanner({ name, isLong }) {
  if (!name) return null;

  return (
    <div className="docchat-banner" role="status">
      <span>
        <span aria-hidden="true">📄 </span>
        Chatting about <strong>{name}</strong>
      </span>
      {isLong && (
        <span className="docchat-warning">
          This document is long, so DocMind reads the parts that match your question.
        </span>
      )}
    </div>
  );
}

export default DocumentBanner;