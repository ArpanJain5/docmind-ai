package com.docmind.backend.dto;

import com.docmind.backend.model.Document;
import com.docmind.backend.model.DocumentStatus;

import java.time.Instant;

// Note what is NOT here: the stored file name, the user and the full text.
public record DocumentResponse(Long id, String originalFilename, long sizeBytes,
                               DocumentStatus status, Integer pageCount, String preview,
                               Instant createdAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getOriginalFilename(),
                document.getSizeBytes(), document.getStatus(), document.getPageCount(),
                document.getPreview(), document.getCreatedAt());
    }
}