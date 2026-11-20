package com.docmind.backend.dto;

// sources is null for a chat that is not about a document
public record ChatResponse(Long conversationId, String reply, SourceInfo sources) {
}