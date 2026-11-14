package com.docmind.backend.repository;

import com.docmind.backend.model.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // "Only this user's chat with this id"
    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    // "All of this user's chats, newest first". The document comes along in the same query
    // (its name is shown in the list), but its big text does not.
    @EntityGraph(attributePaths = "document")
    List<Conversation> findByUserIdOrderByIdDesc(Long userId);

    // When a document is deleted, the chats about it become ordinary chats
    @Modifying
    @Transactional
    @Query("update Conversation c set c.document = null where c.document.id = :documentId")
    int detachDocument(@Param("documentId") Long documentId);
}