package com.docmind.backend.dto;

import com.docmind.backend.model.Conversation;
import com.docmind.backend.model.Document;

import java.time.Instant;

// documentId and documentName are null for an ordinary chat.
// documentTruncated is true when the AI only sees the beginning of the document.
public record ConversationSummary(Long id, String title, Instant createdAt,
                                  Long documentId, String documentName, boolean documentTruncated) {

    public static ConversationSummary from(Conversation conversation, int maxDocumentChars) {
        Document document = conversation.getDocument();
        boolean truncated = document != null
                && document.getCharCount() != null
                && document.getCharCount() > maxDocumentChars;

        return new ConversationSummary(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                document == null ? null : document.getId(),
                document == null ? null : document.getOriginalFilename(),
                truncated);
    }
}