package com.docmind.backend.dto;

public record AuthResponse(String token, long expiresInSeconds, UserResponse user) {
}