package com.docmind.backend.dto;

import com.docmind.backend.model.Conversation;

import java.time.Instant;

public record ConversationSummary(Long id, String title, Instant createdAt) {

    public static ConversationSummary from(Conversation conversation) {
        return new ConversationSummary(
                conversation.getId(), conversation.getTitle(), conversation.getCreatedAt());
    }
}