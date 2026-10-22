package com.docmind.backend.dto;

import com.docmind.backend.model.User;

import java.time.Instant;

public record UserResponse(Long id, String email, String displayName, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}