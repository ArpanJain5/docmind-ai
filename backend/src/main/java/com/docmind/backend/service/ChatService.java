package com.docmind.backend.service;

import org.springframework.stereotype.Service;

@Service
public class ChatService {

    public String generateReply(String userMessage) {
        // Pretend the AI is thinking (real AI calls take 1-5 seconds).
        // We will delete this later.
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "You said: \"" + userMessage + "\". "
                + "(Testing Reply!)";
    }
}