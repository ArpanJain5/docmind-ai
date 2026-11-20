package com.docmind.backend.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentRetrieverTest {

    private static final int BUDGET = 12_000;

    private static String filler(int sentences) {
        return "The weather is mild and the trains run on time. ".repeat(sentences);
    }

    @Test
    void aShortDocumentIsSentWhole() {
        DocumentRetriever.Selection selection = DocumentRetriever.select("short text", "anything", BUDGET);

        assertThat(selection.whole()).isTrue();
        assertThat(selection.text()).isEqualTo("short text");
    }

    @Test
    void aLongDocumentIsNotSentWholeAndStaysInTheBudget() {
        String text = filler(400) + "The final secret word is KANGAROO. " + filler(50);

        DocumentRetriever.Selection selection =
                DocumentRetriever.select(text, "What is the final secret word?", BUDGET);

        assertThat(selection.whole()).isFalse();
        assertThat(selection.text().length()).isLessThanOrEqualTo(BUDGET + 200);
        assertThat(selection.chunksUsed()).isLessThan(selection.chunksInDocument());
    }

    @Test
    void findsAnAnswerNearTheEndOfALongDocument() {
        String text = filler(400) + "The final secret word is KANGAROO. " + filler(50);

        String sent = DocumentRetriever.select(text, "What is the final secret word?", BUDGET).text();

        assertThat(sent).contains("KANGAROO");
    }

    @Test
    void findsAnAnswerInTheMiddleOfALongDocument() {
        String text = filler(200) + "The treasure is buried under the OLIVETREE. " + filler(200);

        String sent = DocumentRetriever.select(text, "Where is the treasure buried?", BUDGET).text();

        assertThat(sent).contains("OLIVETREE");
    }

    @Test
    void aQuestionWithoutUsefulWordsGetsTheBeginningAndTheEnd() {
        String text = "BEGINMARK " + filler(400) + " ENDMARK";

        String sent = DocumentRetriever.select(text, "summarize this document please", BUDGET).text();

        assertThat(sent).contains("BEGINMARK").contains("ENDMARK");
    }

    @Test
    void theBeginningOfTheDocumentIsAlwaysIncluded() {
        String text = filler(400) + "The final secret word is KANGAROO. " + filler(50);

        String sent = DocumentRetriever.select(text, "What is the final secret word?", BUDGET).text();

        assertThat(sent).startsWith(text.substring(0, 50));
    }

    @Test
    void excerptsComeInDocumentOrderWithGapMarkers() {
        String text = "Intro. " + filler(150) + "The ZEBRA crosses here. " + filler(150)
                + "The WALRUS swims here. " + filler(150);

        // The question names the later word first, but the excerpts keep the order of the document
        String sent = DocumentRetriever.select(text, "walrus zebra", BUDGET).text();

        assertThat(sent.indexOf("ZEBRA")).isGreaterThanOrEqualTo(0);
        assertThat(sent.indexOf("WALRUS")).isGreaterThan(sent.indexOf("ZEBRA"));
        assertThat(sent).contains("[...]");
    }

    @Test
    void touchingChunksAreMergedWithoutRepeatingText() {
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= 400; i++) {
            text.append("Sentence number ").append(i).append(" is here. ");
        }

        String sent = DocumentRetriever.select(text.toString(), "Which sentence says number 200?", 6000).text();

        Matcher matcher = Pattern.compile("number (\\d+) is").matcher(sent);
        Set<String> seen = new HashSet<>();
        int found = 0;
        while (matcher.find()) {
            found++;
            assertThat(seen.add(matcher.group(1))).as("sentence %s appears twice", matcher.group(1)).isTrue();
        }
        assertThat(found).isGreaterThan(5);
        assertThat(seen).contains("200");
    }

    @Test
    void theTokenizerDropsCommonAndVeryShortWords() {
        List<String> words = DocumentRetriever.tokenize("What is the Secret word? a 7");

        assertThat(words).containsExactly("secret", "word");
    }

    @Test
    void theSelectionRemembersWhereItsTextComesFrom() {
        String text = filler(400) + "The final secret word is KANGAROO. " + filler(50);

        DocumentRetriever.Selection selection =
                DocumentRetriever.select(text, "What is the final secret word?", BUDGET);

        int secret = text.indexOf("KANGAROO");
        assertThat(selection.runs()).anyMatch(run -> run.start() <= secret && secret < run.end());
        assertThat(selection.runs().get(0).start()).isZero(); // the beginning is always included
    }

    @Test
    void aWholeDocumentIsOneRun() {
        DocumentRetriever.Selection selection = DocumentRetriever.select("short", "anything", BUDGET);

        assertThat(selection.runs()).containsExactly(new DocumentRetriever.Run(0, 5));
    }
}