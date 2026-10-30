import { useEffect, useState } from "react";
import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { copyText } from "../utils/clipboard";

// Links from AI text open in a new tab and cannot access our page
const markdownComponents = {
  a: ({ href, children }) => (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
    </a>
  ),
};

function MessageBubble({ message }) {
  const { role, text, error, welcome, streaming } = message;
  const [copied, setCopied] = useState(false);

  // Show "Copied!" for a moment, then go back to "Copy"
  useEffect(() => {
    if (!copied) return;
    const timer = setTimeout(() => setCopied(false), 1500);
    return () => clearTimeout(timer);
  }, [copied]);

  async function handleCopy() {
    if (await copyText(text)) {
      setCopied(true);
    }
  }

  const isAiReply = role === "assistant" && !error;
  const canCopy = isAiReply && !welcome && !streaming;

  return (
    <div className={`bubble ${role}${error ? " error" : ""}`}>
      {isAiReply ? (
        <div className="markdown">
          <Markdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
            {text}
          </Markdown>
        </div>
      ) : (
        text
      )}
      {canCopy && (
        <button className="copy-btn" onClick={handleCopy}>
          {copied ? "Copied!" : "Copy"}
        </button>
      )}
    </div>
  );
}

export default MessageBubble;