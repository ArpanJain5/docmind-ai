package com.docmind.backend.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiService implements AiService {

    @Override
    public String complete(String systemPrompt, List<AiMessage> history) {
        // Pretend the AI is thinking
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String lastMessage = history.isEmpty() ? "" : history.get(history.size() - 1).content();
        return "You said: \"" + lastMessage + "\". (Fake AI reply. Set app.ai.provider=anthropic for the real one.)";
    }
}