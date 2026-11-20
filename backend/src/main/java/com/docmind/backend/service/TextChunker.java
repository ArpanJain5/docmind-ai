package com.docmind.backend.service;

import java.util.ArrayList;
import java.util.List;

// Cuts a long text into overlapping pieces ("chunks") of about the same size.
// A chunk only remembers WHERE it is in the text (start and end), it does not copy the text.
final class TextChunker {

    record Chunk(int index, int start, int end) {
        int length() {
            return end - start;
        }
    }

    private TextChunker() {
    }

    // size: about how many characters per chunk. overlap: how much each chunk repeats of the one before,
    // so an answer that sits on the border of two chunks is not lost.
    static List<Chunk> split(String text, int size, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + size, length);

            if (end < length) {
                // Prefer to end at a paragraph, a sentence or a word, in the last 30% of the piece
                int cut = lastBreak(text, start + (int) (size * 0.7), end);
                if (cut > start) {
                    end = cut;
                }
                // Never cut an emoji (two characters) in half
                if (end > start + 1 && Character.isHighSurrogate(text.charAt(end - 1))) {
                    end--;
                }
            }

            chunks.add(new Chunk(chunks.size(), start, end));
            if (end >= length) {
                break;
            }

            // The next chunk starts a little before this one ends, at the beginning of a word
            int next = Math.max(end - overlap, start + 1);
            while (next < end && !Character.isWhitespace(text.charAt(next - 1))) {
                next++;
            }
            start = next;
        }
        return chunks;
    }

    // The best place to cut between "from" and "to": after a paragraph break, else after a sentence,
    // else after a space or line break. Returns -1 when there is none.
    private static int lastBreak(String text, int from, int to) {
        for (int i = to - 1; i > from; i--) {
            // The end of a page (a page break character) or an empty line
            if (text.charAt(i) == '\f' || (text.charAt(i) == '\n' && text.charAt(i - 1) == '\n')) {
                return i + 1;
            }
        }
        for (int i = to - 1; i > from; i--) {
            char c = text.charAt(i - 1);
            if ((c == '.' || c == '!' || c == '?') && Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        for (int i = to - 1; i > from; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        return -1;
    }
}