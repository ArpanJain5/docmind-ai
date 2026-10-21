package com.docmind.backend.repository;

import com.docmind.backend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}