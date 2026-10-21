package com.docmind.backend.dto;

public record ChatRequest(Long conversationId, String message) {
}