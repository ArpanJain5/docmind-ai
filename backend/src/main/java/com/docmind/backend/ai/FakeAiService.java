package com.docmind.backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiService implements AiService {

    @Override
    public String complete(String systemPrompt, List<AiMessage> history) {
        pause(800); // pretend the AI is thinking
        return buildReply(history);
    }

    @Override
    public String stream(String systemPrompt, List<AiMessage> history, Consumer<String> onToken) {
        pause(400); // time until the first word
        String reply = buildReply(history);

        // Send the reply word by word, keeping the spaces
        for (String piece : reply.split("(?<= )")) {
            onToken.accept(piece);
            pause(60);
        }
        return reply;
    }

    private String buildReply(List<AiMessage> history) {
        String lastMessage = history.isEmpty() ? "" : history.get(history.size() - 1).content();
        return "You said: \"" + lastMessage + "\". (Fake AI reply. Set app.ai.provider=groq for the real one.)";
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}