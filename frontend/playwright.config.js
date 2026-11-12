import { defineConfig } from "@playwright/test";

// The robot test needs BOTH servers running: the backend on 8080 and the frontend on 5173.
export default defineConfig({
  testDir: "./e2e",
  testMatch: "**/*.e2e.js", // the name keeps these files away from Vitest
  timeout: 30_000,
  reporter: "list",
  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure", // a recording of what the robot saw, only when a test fails
  },
});