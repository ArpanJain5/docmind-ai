import { expect } from "@playwright/test";

// Every test starts as a brand new person, so tests never disturb each other
export async function registerAndOpenDocuments(page) {
  const email = `e2e-${Date.now()}-${Math.floor(Math.random() * 100000)}@example.com`;

  await page.goto("/register");
  await page.getByLabel("Name").fill("Robot");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill("Robot-Pass-123");
  await page.getByRole("button", { name: "Create account" }).click();

  await expect(page).toHaveURL(/\/chat/);
  await page.getByRole("link", { name: "Documents" }).click();
  await expect(page.getByRole("heading", { name: "My documents" })).toBeVisible();
  await expect(page.getByText("No documents yet")).toBeVisible();
}