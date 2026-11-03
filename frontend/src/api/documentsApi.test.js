import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { stubStorage } from "../test/helpers";
import { setUnauthorizedHandler } from "./http";
import { MAX_UPLOAD_BYTES, uploadDocument, validateFile } from "./documentsApi";

// A tiny stand-in for XMLHttpRequest. The test decides how the "server" answers.
class FakeXhr {
  static last = null;
  static plan = { status: 201, body: "{}", fail: false, progress: [] };

  constructor() {
    this.upload = {};
    this.headers = {};
    FakeXhr.last = this;
  }
  open(method, url) {
    this.method = method;
    this.url = url;
  }
  setRequestHeader(name, value) {
    this.headers[name] = value;
  }
  send(body) {
    this.body = body;
    const plan = FakeXhr.plan;
    for (const [loaded, total] of plan.progress) {
      this.upload.onprogress({ lengthComputable: true, loaded, total });
    }
    if (plan.fail) {
      this.onerror();
      return;
    }
    this.status = plan.status;
    this.responseText = plan.body;
    this.onload();
  }
}

function pdf(name = "notes.pdf", size = 100) {
  return new File([new Uint8Array(size)], name, { type: "application/pdf" });
}

beforeEach(() => {
  vi.stubGlobal("XMLHttpRequest", FakeXhr);
  FakeXhr.plan = { status: 201, body: "{}", fail: false, progress: [] };
});

afterEach(() => {
  vi.unstubAllGlobals();
  setUnauthorizedHandler(null);
});

describe("validateFile", () => {
  it("accepts a normal PDF", () => {
    expect(validateFile(pdf())).toBeNull();
  });

  it("rejects other types, empty files and files over the limit", () => {
    expect(validateFile(pdf("photo.png"))).toMatch(/PDF/);
    expect(validateFile(pdf("empty.pdf", 0))).toMatch(/empty/);
    expect(validateFile(pdf("big.pdf", MAX_UPLOAD_BYTES + 1))).toMatch(/too large/);
  });
});

describe("uploadDocument", () => {
  it("sends the file as multipart with the token and reports progress", async () => {
    stubStorage({ "docmind.token": "abc" });
    FakeXhr.plan = {
      status: 201,
      body: JSON.stringify({ id: 7, originalFilename: "notes.pdf" }),
      fail: false,
      progress: [
        [50, 100],
        [100, 100],
      ],
    };
    const seen = [];

    const created = await uploadDocument(pdf(), { onProgress: (p) => seen.push(p) });

    expect(created.id).toBe(7);
    expect(seen).toEqual([50, 100]);
    expect(FakeXhr.last.method).toBe("POST");
    expect(FakeXhr.last.url).toMatch(/\/api\/documents$/);
    expect(FakeXhr.last.headers.Authorization).toBe("Bearer abc");
    expect(FakeXhr.last.headers["Content-Type"]).toBeUndefined(); // the browser adds the boundary
    expect(FakeXhr.last.body).toBeInstanceOf(FormData);
    expect(FakeXhr.last.body.get("file").name).toBe("notes.pdf");
  });

  it("turns server refusals into friendly errors", async () => {
    stubStorage();
    FakeXhr.plan.status = 415;
    await expect(uploadDocument(pdf())).rejects.toMatchObject({ status: 415, message: /PDF/ });

    FakeXhr.plan.status = 413;
    await expect(uploadDocument(pdf())).rejects.toMatchObject({ status: 413, message: /too large/ });
  });

  it("reports a network failure as status 0", async () => {
    stubStorage();
    FakeXhr.plan.fail = true;
    await expect(uploadDocument(pdf())).rejects.toMatchObject({ status: 0 });
  });

  it("logs the user out when the server answers 401 to a request with a token", async () => {
    stubStorage({ "docmind.token": "old" });
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    FakeXhr.plan.status = 401;

    await expect(uploadDocument(pdf())).rejects.toMatchObject({ status: 401 });
    expect(handler).toHaveBeenCalledTimes(1);
  });
});