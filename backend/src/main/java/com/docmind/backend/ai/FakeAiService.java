package com.docmind.backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiService implements AiService {

    private static final String DOCUMENT_START = "<document>";
    private static final String DOCUMENT_END = "</document>";

    @Override
    public String complete(String systemPrompt, List<AiMessage> history) {
        pause(800); // pretend the AI is thinking
        return buildReply(systemPrompt, history);
    }

    @Override
    public String stream(String systemPrompt, List<AiMessage> history, Consumer<String> onToken) {
        pause(400); // time until the first word
        String reply = buildReply(systemPrompt, history);

        // Send the reply word by word, keeping the spaces
        for (String piece : reply.split("(?<= )")) {
            onToken.accept(piece);
            pause(60);
        }
        return reply;
    }

    private String buildReply(String systemPrompt, List<AiMessage> history) {
        String lastMessage = history.isEmpty() ? "" : history.get(history.size() - 1).content();
        return "You said: \"" + lastMessage + "\". (Fake AI reply. Set app.ai.provider=groq for the real one.)"
                + documentHint(systemPrompt, lastMessage);
    }

    // So we can SEE in the app what document text really reached the AI
    private String documentHint(String systemPrompt, String question) {
        if (systemPrompt == null) {
            return "";
        }
        int start = systemPrompt.indexOf(DOCUMENT_START);
        if (start < 0) {
            return "";
        }
        int end = systemPrompt.lastIndexOf(DOCUMENT_END);
        String block = (end > start)
                ? systemPrompt.substring(start + DOCUMENT_START.length(), end)
                : systemPrompt.substring(start + DOCUMENT_START.length());
        String oneLine = block.replaceAll("\\s+", " ").strip();

        boolean excerpts = systemPrompt.contains("you only see excerpts");
        if (!excerpts) {
            String beginning = oneLine.length() > 60 ? oneLine.substring(0, 60) + "..." : oneLine;
            return " [Document attached, it starts with: \"" + beginning + "\"]";
        }

        int parts = block.split("\\[\\.\\.\\.\\]", -1).length;
        return " [Document attached: " + parts + " excerpts, " + oneLine.length() + " characters. "
                + "Words from your question found in them: " + foundWords(question, oneLine) + "]";
    }

    // Which longer words of the question appear in the text the AI received?
    private String foundWords(String question, String text) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (String word : question.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.length() >= 4 && !found.contains(word) && lowerText.contains(word)) {
                found.add(word);
            }
        }
        return found.isEmpty() ? "none" : String.join(", ", found);
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}