import { ApiError, apiUrl, friendlyMessage, getToken, handleUnauthorized, request } from "./http";

// Must match spring.servlet.multipart.max-file-size on the server
export const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

// A quick check in the browser so people get an instant answer.
// The server checks again, because this one can be skipped by anyone.
export function validateFile(file) {
  if (!file.name.toLowerCase().endsWith(".pdf")) return "Please choose a PDF file.";
  if (file.size === 0) return "That file is empty.";
  if (file.size > MAX_UPLOAD_BYTES) return "That file is too large. The limit is 5 MB.";
  return null;
}

export function fetchDocuments() {
  // returns [{ id, originalFilename, sizeBytes, status, createdAt }, ...] newest first
  return request("/documents");
}

export function deleteDocument(id) {
  return request(`/documents/${id}`, { method: "DELETE" });
}

// fetch() cannot report upload progress, so uploads use XMLHttpRequest.
// onProgress gets a number from 0 to 100.
export function uploadDocument(file, { onProgress } = {}) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", apiUrl("/documents"));

    const token = getToken();
    if (token) {
      xhr.setRequestHeader("Authorization", `Bearer ${token}`);
    }
    // We do NOT set Content-Type. The browser adds multipart/form-data and the boundary itself.

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        onProgress(Math.round((event.loaded * 100) / event.total));
      }
    };

    xhr.onload = () => {
      if (xhr.status === 401 && token) {
        handleUnauthorized();
      }
      if (xhr.status < 200 || xhr.status >= 300) {
        reject(new ApiError(xhr.status, friendlyMessage(xhr.status)));
        return;
      }
      try {
        resolve(JSON.parse(xhr.responseText));
      } catch {
        reject(new ApiError(0, "The server sent a reply we could not read."));
      }
    };

    xhr.onerror = () => reject(new ApiError(0, "Cannot reach the server. Is the backend running?"));

    const form = new FormData();
    form.append("file", file);
    xhr.send(form);
  });
}