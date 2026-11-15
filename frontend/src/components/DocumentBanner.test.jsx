// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import DocumentBanner from "./DocumentBanner";

afterEach(cleanup);

describe("DocumentBanner", () => {
  it("shows which document the chat is about", () => {
    render(<DocumentBanner name="report.pdf" truncated={false} />);

    expect(screen.getByText("report.pdf")).toBeTruthy();
    expect(screen.queryByText(/only the first part/)).toBeNull();
  });

  it("warns when the AI only reads the beginning", () => {
    render(<DocumentBanner name="book.pdf" truncated />);

    expect(screen.getByText(/only the first part/)).toBeTruthy();
  });

  it("shows nothing for an ordinary chat", () => {
    const { container } = render(<DocumentBanner name={null} truncated={false} />);

    expect(container.textContent).toBe("");
  });
});