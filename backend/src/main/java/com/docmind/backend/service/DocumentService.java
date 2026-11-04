package com.docmind.backend.service;

import com.docmind.backend.dto.DocumentResponse;
import com.docmind.backend.model.Document;
import com.docmind.backend.model.DocumentStatus;
import com.docmind.backend.model.DocumentText;
import com.docmind.backend.model.User;
import com.docmind.backend.repository.DocumentRepository;
import com.docmind.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    private final PdfTextExtractor extractor;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           FileStorageService storage,
                           PdfTextExtractor extractor) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.extractor = extractor;
    }

    public DocumentResponse upload(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User no longer exists"));

        byte[] bytes = readBytesIfItLooksLikeAPdf(file);                 // 415 if not
        PdfTextExtractor.Extracted extracted = extractor.extract(bytes); // 422 if unreadable
        String storedName = storeOnDisk(bytes);

        try {
            Document saved = documentRepository.save(buildDocument(user, file, storedName, extracted));
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
        documentRepository.delete(document);               // row first (the text goes with it)...
        storage.delete(document.getStoredFilename());      // ...then the file
    }

    // ---------- helpers ----------

    // The first gate: look at the real first bytes, not the file name or the Content-Type
    private byte[] readBytesIfItLooksLikeAPdf(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes(); // at most 5 MB, the multipart limit protects us
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the upload", e);
        }

        boolean startsLikeAPdf = bytes.length >= PDF_MAGIC.length
                && Arrays.equals(bytes, 0, PDF_MAGIC.length, PDF_MAGIC, 0, PDF_MAGIC.length);
        if (!startsLikeAPdf) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are accepted");
        }
        return bytes;
    }

    private String storeOnDisk(byte[] bytes) {
        try {
            return storage.save(bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the file", e);
        }
    }

    private Document buildDocument(User user, MultipartFile file, String storedName,
                                   PdfTextExtractor.Extracted extracted) {
        Document document = new Document(user, cleanName(file.getOriginalFilename()),
                storedName, file.getSize());
        document.setPageCount(extracted.pageCount());

        if (extracted.text().isBlank()) {
            document.setStatus(DocumentStatus.NO_TEXT); // a valid PDF, but nothing to read (a scan)
        } else {
            document.setStatus(DocumentStatus.READY);
            document.setPreview(makePreview(extracted.text()));
            document.setText(new DocumentText(extracted.text()));
        }
        return document;
    }

    // The first 300 characters on one line
    static String makePreview(String text) {
        String oneLine = text.replaceAll("\\s+", " ").strip();
        return oneLine.length() > 300 ? oneLine.substring(0, 300) : oneLine;
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