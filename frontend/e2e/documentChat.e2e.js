import { fileURLToPath } from "node:url";
import { expect, test } from "@playwright/test";
import { registerAndOpenDocuments } from "./helpers.js";

const ARTICLE = fileURLToPath(new URL("./fixtures/article.pdf", import.meta.url));
const BLANK = fileURLToPath(new URL("./fixtures/blank.pdf", import.meta.url));
const LONG = fileURLToPath(new URL("./fixtures/long.pdf", import.meta.url));

// These tests need the backend to answer chat messages (the fake AI or a working real one)

test("a chat about a document shows which document it is about, and survives deleting the document", async ({ page }) => {
  await registerAndOpenDocuments(page);
  await page.locator("#pdf-input").setInputFiles(ARTICLE);
  await expect(page.getByText("Text ready")).toBeVisible();

  await page.getByRole("link", { name: "Back to chat" }).click();
  await page.getByLabel("Chat about a document").selectOption({ label: "article.pdf (3 pages)" });
  await page.getByLabel("Your message").fill("What is this document about?");
  await page.getByRole("button", { name: "Send" }).click();

  // The banner and the chat in the sidebar (the reply can take a few seconds)
  await expect(page.locator(".docchat-banner")).toContainText("article.pdf", { timeout: 20_000 });
  await expect(page.getByRole("button", { name: /What is this document about/ })).toBeVisible({ timeout: 20_000 });
  await expect(page.getByLabel("Chat about a document")).toHaveCount(0);

  // Delete the document: the chat stays, but it is an ordinary chat now
  await page.getByRole("link", { name: "Documents" }).click();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Delete article.pdf" }).click();
  await expect(page.getByText("No documents yet")).toBeVisible();

  await page.getByRole("link", { name: "Back to chat" }).click();
  await expect(page.getByRole("button", { name: /What is this document about/ })).toBeVisible();
  await expect(page.locator(".docchat-banner")).toHaveCount(0);
});

test("a PDF without text is not offered for a chat", async ({ page }) => {
  await registerAndOpenDocuments(page);
  await page.locator("#pdf-input").setInputFiles(BLANK);
  await expect(page.getByText("No text found", { exact: true })).toBeVisible();

  await page.getByRole("link", { name: "Back to chat" }).click();

  await expect(page.getByText(/upload one with readable text/)).toBeVisible();
  await expect(page.getByLabel("Chat about a document")).toHaveCount(0);
});

test("a long document warns that only the first part is read", async ({ page }) => {
  await registerAndOpenDocuments(page);
  await page.locator("#pdf-input").setInputFiles(LONG);
  await expect(page.getByText("Text ready")).toBeVisible();

  await page.getByRole("link", { name: "Back to chat" }).click();
  await page.getByLabel("Chat about a document").selectOption({ label: "long.pdf (14 pages)" });
  await expect(page.getByText("This document is long")).toBeVisible();

  await page.getByLabel("Your message").fill("Summarize this document");
  await page.getByRole("button", { name: "Send" }).click();

  await expect(page.locator(".docchat-banner")).toContainText("only the first part", { timeout: 20_000 });
});