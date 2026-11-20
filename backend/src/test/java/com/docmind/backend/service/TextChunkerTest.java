package com.docmind.backend.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private static String filler(int sentences) {
        return "The weather is mild and the trains run on time. ".repeat(sentences);
    }

    @Test
    void aShortTextIsOneChunk() {
        List<TextChunker.Chunk> chunks = TextChunker.split("hello world", 1000, 150);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).start()).isZero();
        assertThat(chunks.get(0).end()).isEqualTo(11);
    }

    @Test
    void anEmptyTextGivesNoChunks() {
        assertThat(TextChunker.split("", 1000, 150)).isEmpty();
    }

    @Test
    void chunksCoverTheWholeTextWithoutGaps() {
        String text = filler(120); // about 5,800 characters

        List<TextChunker.Chunk> chunks = TextChunker.split(text, 1000, 150);

        assertThat(chunks.get(0).start()).isZero();
        assertThat(chunks.get(chunks.size() - 1).end()).isEqualTo(text.length());
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).start()).isLessThanOrEqualTo(chunks.get(i - 1).end());
        }
    }

    @Test
    void chunksHaveAboutTheRequestedSizeAndOverlapTheOneBefore() {
        List<TextChunker.Chunk> chunks = TextChunker.split(filler(120), 1000, 150);

        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).length()).isLessThanOrEqualTo(1000);
            if (i < chunks.size() - 1) {
                assertThat(chunks.get(i).length()).isGreaterThanOrEqualTo(700);
            }
            if (i > 0) {
                assertThat(chunks.get(i).start()).isLessThan(chunks.get(i - 1).end());
            }
        }
    }

    @Test
    void chunksStartAndEndAtWordBorders() {
        String text = filler(120);

        List<TextChunker.Chunk> chunks = TextChunker.split(text, 1000, 150);

        for (int i = 0; i < chunks.size(); i++) {
            if (i < chunks.size() - 1) {
                assertThat(Character.isWhitespace(text.charAt(chunks.get(i).end() - 1))).isTrue();
            }
            if (i > 0) {
                assertThat(Character.isWhitespace(text.charAt(chunks.get(i).start() - 1))).isTrue();
            }
        }
    }

    @Test
    void anEmojiIsNeverCutInHalf() {
        String text = "a".repeat(999) + "\uD83D\uDE00" + "b".repeat(2000); // the emoji sits on the border

        for (TextChunker.Chunk chunk : TextChunker.split(text, 1000, 150)) {
            assertThat(Character.isHighSurrogate(text.charAt(chunk.end() - 1))).isFalse();
            assertThat(Character.isLowSurrogate(text.charAt(chunk.start()))).isFalse();
        }
    }

    @Test
    void aTextWithoutAnySpaceStillEnds() {
        String text = "x".repeat(5000);

        List<TextChunker.Chunk> chunks = TextChunker.split(text, 1000, 150);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(5);
        assertThat(chunks.get(chunks.size() - 1).end()).isEqualTo(5000);
    }

    @Test
    void chunksPreferToEndAtAPageBreak() {
        String page = "a ".repeat(400); // a page of 800 characters
        String text = page + "\f" + page + "\f" + page + "\f";

        List<TextChunker.Chunk> chunks = TextChunker.split(text, 1000, 150);

        assertThat(text.charAt(chunks.get(0).end() - 1)).isEqualTo('\f');
    }
}