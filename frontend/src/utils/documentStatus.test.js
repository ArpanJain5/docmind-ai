import { describe, expect, it } from "vitest";
import { describeStatus } from "./documentStatus";

describe("describeStatus", () => {
  it("knows the three statuses", () => {
    expect(describeStatus("READY")).toEqual({ label: "Text ready", tone: "ready" });
    expect(describeStatus("NO_TEXT")).toEqual({ label: "No text found", tone: "warn" });
    expect(describeStatus("UPLOADED")).toEqual({ label: "Not read yet", tone: "muted" });
  });

  it("does not break on a status it has never seen", () => {
    expect(describeStatus("SOMETHING_NEW")).toEqual({ label: "SOMETHING_NEW", tone: "muted" });
  });
});