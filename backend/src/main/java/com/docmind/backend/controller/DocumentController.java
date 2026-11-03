package com.docmind.backend.controller;

import com.docmind.backend.dto.DocumentResponse;
import com.docmind.backend.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // The user id comes from the verified token, never from the request
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam("file") MultipartFile file) {
        DocumentResponse created = documentService.upload(Long.valueOf(jwt.getSubject()), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<DocumentResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return documentService.list(Long.valueOf(jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        documentService.delete(Long.valueOf(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }
}