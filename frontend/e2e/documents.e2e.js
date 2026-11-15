import { Buffer } from "node:buffer";
import { fileURLToPath } from "node:url";
import { expect, test } from "@playwright/test";
import { registerAndOpenDocuments } from "./helpers.js";

const ARTICLE = fileURLToPath(new URL("./fixtures/article.pdf", import.meta.url));
const BLANK = fileURLToPath(new URL("./fixtures/blank.pdf", import.meta.url));

test("a text PDF shows its status, pages and preview, survives a refresh, and can be deleted", async ({ page }) => {
  await registerAndOpenDocuments(page);

  await page.locator("#pdf-input").setInputFiles(ARTICLE);

  await expect(page.getByText("Text ready")).toBeVisible();
  await expect(page.getByText("3 pages")).toBeVisible();
  await expect(page.getByText("DocMind test article, page 1")).toBeVisible();

  await page.reload();
  await expect(page.getByText("Text ready")).toBeVisible();

  page.once("dialog", (dialog) => dialog.accept()); // the "Delete this?" question
  await page.getByRole("button", { name: "Delete article.pdf" }).click();
  await expect(page.getByText("No documents yet")).toBeVisible();
});

test("a PDF without text is accepted and explained", async ({ page }) => {
  await registerAndOpenDocuments(page);

  await page.locator("#pdf-input").setInputFiles(BLANK);

  await expect(page.getByText("No text found", { exact: true })).toBeVisible();
  await expect(page.getByText(/looks like a scan/)).toBeVisible();
});

test("a text file pretending to be a PDF is refused", async ({ page }) => {
  await registerAndOpenDocuments(page);

  await page.locator("#pdf-input").setInputFiles({
    name: "fake.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("I am not a PDF"),
  });

  await expect(page.getByRole("alert")).toContainText("Only real PDF files are accepted");
  await expect(page.getByText("No documents yet")).toBeVisible();
});

test("a broken PDF gets a clear message", async ({ page }) => {
  await registerAndOpenDocuments(page);

  await page.locator("#pdf-input").setInputFiles({
    name: "broken.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\nthis is not a real pdf at all\n"),
  });

  await expect(page.getByRole("alert")).toContainText("could not read that PDF");
});

test("a file over 5 MB is stopped in the browser and never sent", async ({ page }) => {
  await registerAndOpenDocuments(page);

  const uploads = [];
  page.on("request", (request) => {
    if (request.method() === "POST" && request.url().endsWith("/api/documents")) uploads.push(request.url());
  });

  await page.locator("#pdf-input").setInputFiles({
    name: "big.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.alloc(5 * 1024 * 1024 + 1),
  });

  await expect(page.getByRole("alert")).toContainText("too large");
  expect(uploads).toHaveLength(0);
});