// 1536 -> "2 KB". Used to show file sizes to people.
export function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// 1 -> "1 page", 3 -> "3 pages"
export function formatPages(count) {
  return count === 1 ? "1 page" : `${count} pages`;
}