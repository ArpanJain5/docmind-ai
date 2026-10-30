package com.docmind.backend.ai;

import java.util.List;
import java.util.function.Consumer;

public interface AiService {

    // Takes the instructions and the recent conversation, and returns the AI's reply
    String complete(String systemPrompt, List<AiMessage> history);

    // Same, but calls onToken with each piece of text as it arrives.
    // Returns the full reply when the stream ends.
    String stream(String systemPrompt, List<AiMessage> history, Consumer<String> onToken);
}