package com.docmind.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

// Spring throws this BEFORE our controller runs when a file is over the limit.
// Without this class the answer depends on framework defaults. With it, it is always 413.
@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() {
        return ResponseEntity.status(413).body(Map.of("message", "The file is too large"));
    }
}