package com.docmind.backend.dto;

import com.docmind.backend.model.Document;
import com.docmind.backend.model.DocumentStatus;

import java.time.Instant;

// Note what is NOT here: the stored file name and the user. The client never needs them.
public record DocumentResponse(Long id, String originalFilename, long sizeBytes,
                               DocumentStatus status, Instant createdAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getOriginalFilename(),
                document.getSizeBytes(), document.getStatus(), document.getCreatedAt());
    }
}