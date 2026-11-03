import { describe, expect, it } from "vitest";
import { formatSize } from "./format";

describe("formatSize", () => {
  it("shows bytes, kilobytes and megabytes", () => {
    expect(formatSize(500)).toBe("500 B");
    expect(formatSize(1536)).toBe("2 KB");
    expect(formatSize(5 * 1024 * 1024)).toBe("5.0 MB");
  });
});