
package com.docmind.backend.repository;

import com.docmind.backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // "Only this user's document with this id" (same idea as findByIdAndUserId for chats)
    Optional<Document> findByIdAndUserId(Long id, Long userId);

    List<Document> findByUserIdOrderByIdDesc(Long userId);
}