// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import DocumentBanner from "./DocumentBanner";

afterEach(cleanup);

describe("DocumentBanner", () => {
  it("shows which document the chat is about", () => {
    render(<DocumentBanner name="report.pdf" isLong={false} />);

    expect(screen.getByText("report.pdf")).toBeTruthy();
    expect(screen.queryByText(/This document is long/)).toBeNull();
  });

  it("explains that a long document is searched for the parts that match each question", () => {
    render(<DocumentBanner name="book.pdf" isLong />);

    expect(screen.getByText(/reads the parts that match your question/)).toBeTruthy();
  });

  it("shows nothing for an ordinary chat", () => {
    const { container } = render(<DocumentBanner name={null} isLong={false} />);

    expect(container.textContent).toBe("");
  });
});