package com.docmind.backend.dto;

import java.util.ArrayList;
import java.util.List;

// Which parts of a document an answer was based on.
//   WHOLE    the AI got the whole document
//   PAGES    the AI got excerpts, and these are the pages they come from
//   EXCERPTS the AI got excerpts, but the page numbers are not known (documents uploaded before Day 19)
public record SourceInfo(Mode mode, List<PageRange> pages) {

    public enum Mode {
        WHOLE, PAGES, EXCERPTS
    }

    public record PageRange(int from, int to) {
    }

    public static SourceInfo whole() {
        return new SourceInfo(Mode.WHOLE, List.of());
    }

    public static SourceInfo excerpts() {
        return new SourceInfo(Mode.EXCERPTS, List.of());
    }

    public static SourceInfo pages(List<PageRange> pages) {
        return new SourceInfo(Mode.PAGES, List.copyOf(pages));
    }

    // The short text saved in the database: "WHOLE", "EXCERPTS" or "PAGES 1-2,14-14"
    public String toStorage() {
        if (mode != Mode.PAGES) {
            return mode.name();
        }
        StringBuilder text = new StringBuilder("PAGES ");
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(pages.get(i).from()).append('-').append(pages.get(i).to());
        }
        return text.toString();
    }

    // The other way round. Anything that is empty or not understood gives null (no sources).
    public static SourceInfo fromStorage(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String text = stored.strip();
        if (text.equals("WHOLE")) {
            return whole();
        }
        if (text.equals("EXCERPTS")) {
            return excerpts();
        }
        if (!text.startsWith("PAGES ")) {
            return null;
        }
        try {
            List<PageRange> pages = new ArrayList<>();
            for (String part : text.substring("PAGES ".length()).split(",")) {
                String[] ends = part.split("-");
                pages.add(new PageRange(Integer.parseInt(ends[0].strip()), Integer.parseInt(ends[1].strip())));
            }
            return pages(pages);
        } catch (RuntimeException e) { // a number that is not a number, or a part without a dash
            return null;
        }
    }
}