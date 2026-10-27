package com.docmind.backend.ai;

import com.docmind.backend.model.Role;

public record AiMessage(Role role, String content) {
}