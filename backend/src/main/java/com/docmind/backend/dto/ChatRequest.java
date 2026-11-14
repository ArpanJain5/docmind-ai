package com.docmind.backend.dto;

// documentId is optional. It is only used when a NEW chat starts (conversationId is null).
public record ChatRequest(Long conversationId, String message, Long documentId) {
}