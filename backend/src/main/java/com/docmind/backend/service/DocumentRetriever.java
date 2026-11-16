package com.docmind.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// Chooses WHICH parts of a long document the AI gets to see for a question.
// Short documents are sent whole. Long ones are cut into chunks, the chunks are ranked by how well
// they match the question (the BM25 formula, a classic keyword search), and the best ones are sent.
final class DocumentRetriever {

    static final int CHUNK_SIZE = 1000;
    static final int CHUNK_OVERLAP = 150;

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    // Very common words say nothing about what a question is looking for
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "of", "to", "in", "on", "at", "for",
            "and", "or", "but", "not", "with", "by", "from", "as", "it", "its", "this", "that", "these",
            "those", "what", "which", "who", "whom", "when", "where", "why", "how", "do", "does", "did",
            "can", "could", "would", "should", "will", "there", "their", "about", "me", "my", "you", "your",
            "i", "we", "they", "he", "she", "has", "have", "had", "tell", "please", "any", "some", "all");

    // text: the text the AI will get. whole: true when it is the complete document.
    record Selection(boolean whole, String text, int chunksInDocument, int chunksUsed) {
    }

    private DocumentRetriever() {
    }

    static Selection select(String documentText, String question, int budgetChars) {
        if (documentText.length() <= budgetChars) {
            return new Selection(true, documentText, 1, 1);
        }

        List<TextChunker.Chunk> chunks = TextChunker.split(documentText, CHUNK_SIZE, CHUNK_OVERLAP);
        double[] scores = score(question, documentText, chunks);

        // Best chunks first (ties: the earlier one)
        List<Integer> ranked = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            if (scores[i] > 0) {
                ranked.add(i);
            }
        }
        ranked.sort(Comparator.<Integer>comparingDouble(i -> scores[i]).reversed().thenComparingInt(i -> i));

        Set<Integer> selected = new LinkedHashSet<>();
        int[] used = {0};

        // The beginning (title, introduction) always helps to understand the rest
        add(0, chunks, selected, used, budgetChars, true);

        if (ranked.isEmpty()) {
            // The question has no useful words (for example "summarize this"): spread over the whole document
            int wanted = Math.max(1, budgetChars / CHUNK_SIZE);
            for (int j = 1; j < wanted; j++) {
                int index = (int) Math.round((double) j * (chunks.size() - 1) / (wanted - 1));
                add(index, chunks, selected, used, budgetChars, false);
            }
        } else {
            for (int index : ranked) {
                add(index, chunks, selected, used, budgetChars, false);
            }
            // Room left: take the neighbours of the best chunks, for the context around an answer
            for (int index : ranked) {
                add(index + 1, chunks, selected, used, budgetChars, false);
                add(index - 1, chunks, selected, used, budgetChars, false);
            }
        }

        return new Selection(false, join(documentText, chunks, selected), chunks.size(), selected.size());
    }

    private static void add(int index, List<TextChunker.Chunk> chunks, Set<Integer> selected, int[] used,
                            int budget, boolean force) {
        if (index < 0 || index >= chunks.size() || selected.contains(index)) {
            return;
        }
        int length = chunks.get(index).length();
        if (force || used[0] + length <= budget) {
            selected.add(index);
            used[0] += length;
        }
    }

    // The chosen chunks in document order. Chunks that touch or overlap become one piece, and
    // pieces with a gap between them are separated by "[...]".
    private static String join(String text, List<TextChunker.Chunk> chunks, Set<Integer> selected) {
        StringBuilder result = new StringBuilder();
        int runStart = -1;
        int runEnd = -1;
        for (int index : new TreeSet<>(selected)) {
            TextChunker.Chunk chunk = chunks.get(index);
            if (runStart < 0) {
                runStart = chunk.start();
                runEnd = chunk.end();
            } else if (chunk.start() <= runEnd) {
                runEnd = Math.max(runEnd, chunk.end());
            } else {
                result.append(text, runStart, runEnd).append("\n[...]\n");
                runStart = chunk.start();
                runEnd = chunk.end();
            }
        }
        if (runStart >= 0) {
            result.append(text, runStart, runEnd);
        }
        return result.toString();
    }

    // ---------- ranking (BM25) ----------

    static double[] score(String question, String text, List<TextChunker.Chunk> chunks) {
        Set<String> queryTerms = new HashSet<>(tokenize(question));
        double[] scores = new double[chunks.size()];
        if (queryTerms.isEmpty()) {
            return scores;
        }

        // For every chunk: how often does each question word appear, and how long is the chunk?
        List<Map<String, Integer>> counts = new ArrayList<>();
        int[] lengths = new int[chunks.size()];
        Map<String, Integer> chunksWithTerm = new HashMap<>();
        long totalLength = 0;

        for (int i = 0; i < chunks.size(); i++) {
            TextChunker.Chunk chunk = chunks.get(i);
            List<String> tokens = tokenize(text.substring(chunk.start(), chunk.end()));
            Map<String, Integer> count = new HashMap<>();
            for (String token : tokens) {
                if (queryTerms.contains(token)) {
                    count.merge(token, 1, Integer::sum);
                }
            }
            for (String term : count.keySet()) {
                chunksWithTerm.merge(term, 1, Integer::sum);
            }
            counts.add(count);
            lengths[i] = tokens.size();
            totalLength += tokens.size();
        }

        double averageLength = Math.max(1.0, (double) totalLength / chunks.size());
        int total = chunks.size();

        for (int i = 0; i < total; i++) {
            for (Map.Entry<String, Integer> entry : counts.get(i).entrySet()) {
                int withTerm = chunksWithTerm.get(entry.getKey());
                // A rare word is worth more than a word that is everywhere
                double idf = Math.log(1 + (total - withTerm + 0.5) / (withTerm + 0.5));
                double tf = entry.getValue();
                double norm = tf + K1 * (1 - B + B * lengths[i] / averageLength);
                scores[i] += idf * (tf * (K1 + 1)) / norm;
            }
        }
        return scores;
    }

    // Lower case, split into words and drop the very common ones
    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }
}