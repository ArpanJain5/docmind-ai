package com.docmind.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// The full text of a document. It lives in its own table so that listing documents
// never loads it (a 100-page PDF can hold hundreds of thousands of characters).
@Entity
@Table(name = "document_texts")
@Getter
@Setter
@NoArgsConstructor
public class DocumentText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LONGTEXT says it out loud, so the column is big on every database
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    public DocumentText(String content) {
        this.content = content;
    }
}