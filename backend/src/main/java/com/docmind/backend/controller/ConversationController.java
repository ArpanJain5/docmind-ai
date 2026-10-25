package com.docmind.backend.controller;

import com.docmind.backend.dto.ConversationSummary;
import com.docmind.backend.dto.MessageResponse;
import com.docmind.backend.service.ChatService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ChatService chatService;

    public ConversationController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ConversationSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return chatService.listConversations(Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable Long id) {
        return chatService.getMessages(Long.valueOf(jwt.getSubject()), id);
    }
}