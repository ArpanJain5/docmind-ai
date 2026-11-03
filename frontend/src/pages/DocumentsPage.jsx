import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { useAuth } from "../auth/AuthContext";
import { usePageTitle } from "../hooks/usePageTitle";
import {
  deleteDocument,
  fetchDocuments,
  uploadDocument,
  validateFile,
} from "../api/documentsApi";
import { formatSize } from "../utils/format";
import "./DocumentsPage.css";

function DocumentsPage() {
  usePageTitle("Documents");
  const { user, logout } = useAuth();

  // The list has the usual faces: loading, error, ready (empty or not)
  const [list, setList] = useState({ status: "loading", docs: [], message: "" });
  const [attempt, setAttempt] = useState(0);

  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [actionError, setActionError] = useState("");
  const [deletingId, setDeletingId] = useState(null);
  const fileInput = useRef(null);

  useEffect(() => {
    let cancelled = false;
    fetchDocuments()
      .then((docs) => {
        if (!cancelled) setList({ status: "ready", docs, message: "" });
      })
      .catch((error) => {
        if (!cancelled) setList({ status: "error", docs: [], message: error.message });
      });
    return () => {
      cancelled = true;
    };
  }, [attempt]);

  function retry() {
    setList({ status: "loading", docs: [], message: "" });
    setAttempt((n) => n + 1);
  }

  async function handleFileChosen(event) {
    const file = event.target.files[0];
    event.target.value = ""; // so choosing the same file again still works
    if (!file) return;

    const problem = validateFile(file);
    if (problem) {
      setActionError(problem);
      return;
    }

    setActionError("");
    setProgress(0);
    setUploading(true);
    try {
      const created = await uploadDocument(file, { onProgress: setProgress });
      setList((current) => ({ ...current, docs: [created, ...current.docs] }));
    } catch (error) {
      setActionError(error.message);
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(doc) {
    if (!window.confirm(`Delete "${doc.originalFilename}"? This cannot be undone.`)) return;

    setActionError("");
    setDeletingId(doc.id);
    try {
      await deleteDocument(doc.id);
      setList((current) => ({ ...current, docs: current.docs.filter((d) => d.id !== doc.id) }));
    } catch (error) {
      setActionError(error.message);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="docs-page">
      <div className="docs-header">
        <h1>My documents</h1>
        <div className="docs-actions">
          <span className="docs-user">{user.displayName}</span>
          <Link className="docs-link" to="/chat">
            Back to chat
          </Link>
          <button className="docs-btn" onClick={logout}>
            Log out
          </button>
        </div>
      </div>

      <div className="docs-upload">
        <input
          ref={fileInput}
          id="pdf-input"
          type="file"
          accept="application/pdf,.pdf"
          onChange={handleFileChosen}
          hidden
        />
        <button
          className="primary-btn"
          onClick={() => fileInput.current.click()}
          disabled={uploading || list.status !== "ready"}
        >
          {uploading ? "Uploading..." : "Upload a PDF"}
        </button>
        <span className="docs-hint">PDF only, up to 5 MB</span>

        {uploading && (
          <div className="docs-progress">
            <progress value={progress} max="100" aria-label="Upload progress" />
            <span>{progress}%</span>
          </div>
        )}
      </div>

      {actionError && (
        <p className="docs-error" role="alert">
          {actionError}
        </p>
      )}

      {list.status === "loading" && (
        <p className="docs-status" role="status">
          Loading your documents...
        </p>
      )}

      {list.status === "error" && (
        <div className="docs-status" role="alert">
          <p>{list.message}</p>
          <button className="docs-btn" onClick={retry}>
            Try again
          </button>
        </div>
      )}

      {list.status === "ready" && list.docs.length === 0 && (
        <p className="docs-status">No documents yet. Upload your first PDF above.</p>
      )}

      {list.status === "ready" && list.docs.length > 0 && (
        <ul className="docs-list">
          {list.docs.map((doc) => (
            <li key={doc.id} className="docs-item">
              <div className="docs-info">
                <span className="docs-name">{doc.originalFilename}</span>
                <span className="docs-meta">
                  {formatSize(doc.sizeBytes)} · {new Date(doc.createdAt).toLocaleDateString()}
                </span>
              </div>
              <button
                className="docs-btn docs-delete"
                onClick={() => handleDelete(doc)}
                disabled={deletingId === doc.id}
                aria-label={`Delete ${doc.originalFilename}`}
              >
                {deletingId === doc.id ? "Deleting..." : "Delete"}
                </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default DocumentsPage;