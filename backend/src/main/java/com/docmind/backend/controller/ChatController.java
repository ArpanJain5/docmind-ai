package com.docmind.backend.controller;

import com.docmind.backend.dto.ChatRequest;
import com.docmind.backend.dto.ChatResponse;
import com.docmind.backend.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String message = request.message();
        if (message == null || message.isBlank() || message.length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(chatService.chat(request.conversationId(), message));
    }
}