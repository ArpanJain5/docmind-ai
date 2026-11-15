// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import DocumentPicker from "./DocumentPicker";

afterEach(cleanup);

const report = { id: 3, originalFilename: "report.pdf", status: "READY", pageCount: 2, charCount: 500 };
const scan = { id: 4, originalFilename: "scan.pdf", status: "NO_TEXT", pageCount: 1, charCount: 0 };
const book = { id: 5, originalFilename: "book.pdf", status: "READY", pageCount: 300, charCount: 400000 };

function renderPicker(props) {
  return render(
    <MemoryRouter>
      <DocumentPicker documents={[]} status="ready" selectedId={null} onSelect={() => {}} {...props} />
    </MemoryRouter>
  );
}

describe("DocumentPicker", () => {
  it("offers only documents with readable text, plus the ordinary chat", () => {
    renderPicker({ documents: [report, scan] });

    const options = screen.getAllByRole("option").map((option) => option.textContent);
    expect(options).toEqual(["No document (ordinary chat)", "report.pdf (2 pages)"]);
  });

  it("tells the id of the chosen document, or null for no document", () => {
    const onSelect = vi.fn();
    renderPicker({ documents: [report], onSelect });
    const select = screen.getByLabelText("Chat about a document");

    fireEvent.change(select, { target: { value: "3" } });
    fireEvent.change(select, { target: { value: "" } });

    expect(onSelect).toHaveBeenNthCalledWith(1, 3);
    expect(onSelect).toHaveBeenNthCalledWith(2, null);
  });

  it("points to the Documents page when there is nothing readable", () => {
    renderPicker({ documents: [scan] });

    expect(screen.queryByLabelText("Chat about a document")).toBeNull();
    expect(screen.getByRole("link", { name: /upload one with readable text/ }).getAttribute("href")).toBe(
      "/documents"
    );
  });

  it("explains when the documents could not be loaded, and shows nothing while loading", () => {
    const { unmount } = renderPicker({ status: "error" });
    expect(screen.getByText(/could not be loaded/)).toBeTruthy();
    unmount();

    const { container } = renderPicker({ status: "loading" });
    expect(container.textContent).toBe("");
  });

  it("warns when the chosen document is long", () => {
    renderPicker({ documents: [report, book], selectedId: 5 });
    expect(screen.getByText(/reads only the first part/)).toBeTruthy();
  });

  it("does not warn for a short document", () => {
    renderPicker({ documents: [report, book], selectedId: 3 });
    expect(screen.queryByText(/reads only the first part/)).toBeNull();
  });
});