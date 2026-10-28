package com.docmind.backend.controller;

import com.docmind.backend.dto.ConversationSummary;
import com.docmind.backend.dto.MessageResponse;
import com.docmind.backend.dto.RenameRequest;
import com.docmind.backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PatchMapping("/{id}")
    public ConversationSummary rename(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Long id,
                                      @Valid @RequestBody RenameRequest request) {
        return chatService.renameConversation(Long.valueOf(jwt.getSubject()), id, request.title());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long id) {
        chatService.deleteConversation(Long.valueOf(jwt.getSubject()), id);
        return ResponseEntity.noContent().build(); // 204: done, nothing to send back
    }
}