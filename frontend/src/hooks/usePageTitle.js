import { useEffect } from "react";

// Sets the browser tab title while a page is on screen
export function usePageTitle(title) {
  useEffect(() => {
    const previous = document.title;
    document.title = title ? `${title} · DocMind AI` : "DocMind AI";
    return () => {
      document.title = previous;
    };
  }, [title]);
}