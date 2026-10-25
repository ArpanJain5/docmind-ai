package com.docmind.backend.repository;

import com.docmind.backend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // "Only this user's chat with this id"
    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    // "All of this user's chats, newest first"
    List<Conversation> findByUserIdOrderByIdDesc(Long userId);
}