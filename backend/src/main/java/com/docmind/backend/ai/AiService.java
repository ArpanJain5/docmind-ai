package com.docmind.backend.ai;

import java.util.List;

public interface AiService {

    // Takes the instructions and the recent conversation, and returns the AI's reply
    String complete(String systemPrompt, List<AiMessage> history);
}