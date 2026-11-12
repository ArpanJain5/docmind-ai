// Turns the status word from the server into words and a colour for people.
// "tone" picks a CSS class: ready (green), warn (amber), muted (grey).
export function describeStatus(status) {
  if (status === "READY") return { label: "Text ready", tone: "ready" };
  if (status === "NO_TEXT") return { label: "No text found", tone: "warn" };
  if (status === "UPLOADED") return { label: "Not read yet", tone: "muted" };
  return { label: String(status), tone: "muted" };
}