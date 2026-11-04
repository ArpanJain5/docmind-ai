package com.docmind.backend.model;

public enum DocumentStatus {
    UPLOADED, // old Day 15 rows that were never read
    READY,    // text extracted
    NO_TEXT   // a valid PDF with no text inside (for example a scan)
}