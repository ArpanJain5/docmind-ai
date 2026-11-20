package com.docmind.backend.service;

import com.docmind.backend.dto.SourceInfo.PageRange;

import java.util.ArrayList;
import java.util.List;

// Knows where every page of a document starts and ends.
// The text of a document has a page break character (form feed) after every page.
final class PageMap {

    static final char PAGE_BREAK = '\f';

    private final int[] breaks; // the positions of the page breaks

    PageMap(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == PAGE_BREAK) {
                count++;
            }
        }
        breaks = new int[count];
        int next = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == PAGE_BREAK) {
                breaks[next++] = i;
            }
        }
    }

    // False for documents uploaded before Day 19: their text has no page breaks
    boolean hasPages() {
        return breaks.length > 0;
    }

    // The page (counting from 1) that contains the character at this position
    int pageOf(int offset) {
        int low = 0;
        int high = breaks.length;
        while (low < high) { // how many page breaks are before this position?
            int middle = (low + high) >>> 1;
            if (breaks[middle] < offset) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low + 1;
    }

    // The pages the given pieces of text come from, in order. Pages that touch are joined: 1-2, 14-14
    List<PageRange> pagesOf(List<DocumentRetriever.Run> runs) {
        List<PageRange> pages = new ArrayList<>();
        for (DocumentRetriever.Run run : runs) {
            int from = pageOf(run.start());
            int to = pageOf(Math.max(run.start(), run.end() - 1));
            int last = pages.size() - 1;
            if (last >= 0 && from <= pages.get(last).to() + 1) {
                PageRange previous = pages.remove(last);
                pages.add(new PageRange(previous.from(), Math.max(previous.to(), to)));
            } else {
                pages.add(new PageRange(from, to));
            }
        }
        return pages;
    }
}