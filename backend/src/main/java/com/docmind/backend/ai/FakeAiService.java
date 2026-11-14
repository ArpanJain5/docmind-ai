package com.docmind.backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
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
                + documentHint(systemPrompt);
    }

    // So we can SEE in the app that the document text really reached the AI
    private String documentHint(String systemPrompt) {
        int start = systemPrompt == null ? -1 : systemPrompt.indexOf(DOCUMENT_START);
        if (start < 0) {
            return "";
        }
        int end = systemPrompt.lastIndexOf(DOCUMENT_END);
        String text = (end > start)
                ? systemPrompt.substring(start + DOCUMENT_START.length(), end)
                : systemPrompt.substring(start + DOCUMENT_START.length());
        String oneLine = text.replaceAll("\\s+", " ").strip();
        String beginning = oneLine.length() > 60 ? oneLine.substring(0, 60) + "..." : oneLine;
        return " [Document attached, it starts with: \"" + beginning + "\"]";
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}