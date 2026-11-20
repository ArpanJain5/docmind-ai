package com.docmind.backend.dto;

import com.docmind.backend.model.Message;

import java.time.Instant;
import java.util.Locale;

// sources is only set for answers about a document
public record MessageResponse(Long id, String role, String content, Instant createdAt, SourceInfo sources) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole().name().toLowerCase(Locale.ROOT),
                message.getContent(),
                message.getCreatedAt(),
                SourceInfo.fromStorage(message.getSources()));
    }
}