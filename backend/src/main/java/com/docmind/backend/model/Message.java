package com.docmind.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // TEXT, because the default column (255 characters) would crash on long messages
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Message(Conversation conversation, Role role, String content) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
    }
}