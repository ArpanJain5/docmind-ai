package com.docmind.backend.repository;

import com.docmind.backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // "Only this user's document with this id" (same idea as findByIdAndUserId for chats)
    Optional<Document> findByIdAndUserId(Long id, Long userId);

    List<Document> findByUserIdOrderByIdDesc(Long userId);

    // The full text, and nothing else. Again only for the owner.
    @Query("select t.content from Document d join d.text t where d.id = :documentId and d.user.id = :userId")
    Optional<String> findTextByIdAndUserId(@Param("documentId") Long documentId,
                                           @Param("userId") Long userId);
}