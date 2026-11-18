import { Link } from "react-router";
import { LONG_DOCUMENT_CHARS } from "../api/documentsApi";
import { formatPages } from "../utils/format";
import "./DocumentChat.css";

// Lets the person choose a document for a NEW chat. Only documents with readable text are offered.
// status is "loading", "error" or "ready". onSelect gets the document id, or null for "no document".
function DocumentPicker({ documents, status, selectedId, onSelect }) {
  if (status === "loading") return null;

  if (status === "error") {
    return (
      <p className="docchat-hint">
        Your documents could not be loaded, so you can only start an ordinary chat.
      </p>
    );
  }

  const readable = documents.filter((doc) => doc.status === "READY");

  if (readable.length === 0) {
    return (
      <p className="docchat-hint">
        To chat about a PDF, <Link to="/documents">upload one with readable text</Link>.
      </p>
    );
  }

  const selected = readable.find((doc) => doc.id === selectedId);
  const isLong = selected && selected.charCount > LONG_DOCUMENT_CHARS;

  return (
    <div className="docchat-picker">
      <label htmlFor="document-select">Chat about a document</label>
      <select
        id="document-select"
        value={selectedId ?? ""}
        onChange={(event) => onSelect(event.target.value === "" ? null : Number(event.target.value))}
      >
        <option value="">No document (ordinary chat)</option>
        {readable.map((doc) => (
          <option key={doc.id} value={doc.id}>
            {doc.pageCount != null
              ? `${doc.originalFilename} (${formatPages(doc.pageCount)})`
              : doc.originalFilename}
          </option>
        ))}
      </select>
      {isLong && (
        <p className="docchat-hint">
          This document is long. DocMind searches it and reads the parts that match your question.
        </p>
      )}
    </div>
  );
}

export default DocumentPicker;