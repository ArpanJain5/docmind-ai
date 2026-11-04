package com.docmind.backend.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Every document belongs to exactly one user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The name the person gave it. For showing on screen ONLY, never used as a path.
    @Column(nullable = false, length = 255)
    private String originalFilename;

    // The random name on disk, for example 3f2b...-....pdf
    @Column(nullable = false, unique = true, length = 64)
    private String storedFilename;

    @Column(nullable = false)
    private long sizeBytes;

    // VARCHAR on purpose: Hibernate would otherwise create a MySQL enum(...) column
    // that "update" can never extend when we add a new status.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    // null for old Day 15 rows that were never read
    private Integer pageCount;

    // The first 300 characters, for showing in lists
    @Column(length = 300)
    private String preview;

    // The full text. LAZY: only loaded when someone really asks for it.
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "text_id")
    private DocumentText text;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Document(User user, String originalFilename, String storedFilename, long sizeBytes) {
        this.user = user;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.sizeBytes = sizeBytes;
    }
}