package com.docmind.backend.service;

import com.docmind.backend.dto.DocumentResponse;
import com.docmind.backend.model.Document;
import com.docmind.backend.model.User;
import com.docmind.backend.repository.DocumentRepository;
import com.docmind.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class DocumentService {

    // Every real PDF starts with these five characters
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           FileStorageService storage) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    public DocumentResponse upload(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User no longer exists"));

        String storedName = storeIfItIsAPdf(file);

        try {
            Document saved = documentRepository.save(
                    new Document(user, cleanName(file.getOriginalFilename()), storedName, file.getSize()));
            return DocumentResponse.from(saved);
        } catch (RuntimeException e) {
            storage.delete(storedName); // do not leave a file with no database row
            throw e;
        }
    }

    public List<DocumentResponse> list(Long userId) {
        return documentRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public void delete(Long userId, Long documentId) {
        Document document = findOwned(userId, documentId); // 404 unless it is YOURS
        documentRepository.delete(document);               // row first...
        storage.delete(document.getStoredFilename());      // ...then the file
    }

    // ---------- helpers ----------

    // Looks at the real first bytes, not the file name or the Content-Type (both are just claims)
    private String storeIfItIsAPdf(MultipartFile file) {
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            in.mark(PDF_MAGIC.length);
            byte[] head = in.readNBytes(PDF_MAGIC.length);
            in.reset();

            if (!Arrays.equals(head, PDF_MAGIC)) {
                throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are accepted");
            }
            return storage.save(in);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the file", e);
        }
    }

    // "../../evil.pdf" becomes "evil.pdf". The name is for display only, but we clean it anyway.
    static String cleanName(String raw) {
        String name = raw == null ? "" : raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty()) {
            name = "document.pdf";
        }
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private Document findOwned(Long userId, Long documentId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));
    }
}