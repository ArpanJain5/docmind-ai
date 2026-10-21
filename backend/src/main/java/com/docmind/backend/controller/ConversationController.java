package com.docmind.backend.controller;

import com.docmind.backend.dto.MessageResponse;
import com.docmind.backend.service.ChatService;
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

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id) {
        return chatService.getMessages(id);
    }
}