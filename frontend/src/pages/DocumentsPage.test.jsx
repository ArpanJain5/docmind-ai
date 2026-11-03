// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { AuthContext } from "../auth/AuthContext";
import DocumentsPage from "./DocumentsPage";
import * as api from "../api/documentsApi";

vi.mock("../api/documentsApi", async (importOriginal) => {
  const real = await importOriginal();
  return { ...real, fetchDocuments: vi.fn(), uploadDocument: vi.fn(), deleteDocument: vi.fn() };
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

const doc = {
  id: 1,
  originalFilename: "report.pdf",
  sizeBytes: 2048,
  status: "UPLOADED",
  createdAt: "2026-11-03T10:00:00Z",
};

function renderPage() {
  const auth = { user: { displayName: "Arpan" }, logout() {} };
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter>
        <DocumentsPage />
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

function choose(file) {
  const input = document.getElementById("pdf-input");
  fireEvent.change(input, { target: { files: [file] } });
}

describe("DocumentsPage", () => {
  it("shows the empty state", async () => {
    api.fetchDocuments.mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText(/No documents yet/)).toBeTruthy();
  });

  it("shows the documents", async () => {
    api.fetchDocuments.mockResolvedValue([doc]);
    renderPage();
    expect(await screen.findByText("report.pdf")).toBeTruthy();
    expect(screen.getByText(/2 KB/)).toBeTruthy();
  });

  it("shows an error with a retry that works", async () => {
    api.fetchDocuments.mockRejectedValueOnce(new Error("Cannot reach the server."));
    api.fetchDocuments.mockResolvedValueOnce([doc]);
    renderPage();

    expect(await screen.findByText("Cannot reach the server.")).toBeTruthy();
    fireEvent.click(screen.getByText("Try again"));
    expect(await screen.findByText("report.pdf")).toBeTruthy();
  });

  it("refuses a non-PDF in the browser without calling the server", async () => {
    api.fetchDocuments.mockResolvedValue([]);
    renderPage();
    await screen.findByText(/No documents yet/);

    choose(new File(["hi"], "photo.png", { type: "image/png" }));

    expect((await screen.findByRole("alert")).textContent).toMatch(/PDF/);
    expect(api.uploadDocument).not.toHaveBeenCalled();
  });

  it("uploads a PDF and adds it to the top of the list", async () => {
    api.fetchDocuments.mockResolvedValue([]);
    api.uploadDocument.mockResolvedValue(doc);
    renderPage();
    await screen.findByText(/No documents yet/);

    choose(new File(["%PDF-1.4"], "report.pdf", { type: "application/pdf" }));

    expect(await screen.findByText("report.pdf")).toBeTruthy();
    expect(api.uploadDocument).toHaveBeenCalledTimes(1);
  });

  it("shows the server's message when an upload fails", async () => {
    api.fetchDocuments.mockResolvedValue([]);
    api.uploadDocument.mockRejectedValue(new Error("Only real PDF files are accepted."));
    renderPage();
    await screen.findByText(/No documents yet/);

    choose(new File(["not a pdf"], "fake.pdf", { type: "application/pdf" }));

    expect((await screen.findByRole("alert")).textContent).toMatch(/real PDF/);
  });

  it("deletes a document after confirmation", async () => {
    api.fetchDocuments.mockResolvedValue([doc]);
    api.deleteDocument.mockResolvedValue(null);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPage();

    fireEvent.click(await screen.findByLabelText("Delete report.pdf"));

    await waitFor(() => expect(screen.queryByText("report.pdf")).toBeNull());
    expect(api.deleteDocument).toHaveBeenCalledWith(1);
  });

  it("does nothing when the person cancels the delete", async () => {
    api.fetchDocuments.mockResolvedValue([doc]);
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderPage();

    fireEvent.click(await screen.findByLabelText("Delete report.pdf"));

    expect(api.deleteDocument).not.toHaveBeenCalled();
    expect(screen.getByText("report.pdf")).toBeTruthy();
  });
});