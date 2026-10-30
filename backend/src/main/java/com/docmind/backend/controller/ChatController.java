package com.docmind.backend.controller;

import com.docmind.backend.dto.ChatRequest;
import com.docmind.backend.dto.ChatResponse;
import com.docmind.backend.service.ChatService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final ChatService chatService;

    // Each stream gets its own cheap virtual thread (Java 21)
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdown();
    }

    // The whole reply at once (kept for simple clients and tests)
    @PostMapping
    public ChatResponse chat(@AuthenticationPrincipal Jwt jwt, @RequestBody ChatRequest request) {
        String message = requireValidMessage(request);
        Long userId = Long.valueOf(jwt.getSubject());
        return chatService.chat(userId, request.conversationId(), message);
    }

    // The reply piece by piece, as Server-Sent Events
    @PostMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal Jwt jwt, @RequestBody ChatRequest request) {
        String message = requireValidMessage(request);
        Long userId = Long.valueOf(jwt.getSubject());

        // Checked BEFORE streaming starts, so a stranger's chat id gets a real 404
        chatService.verifyAccess(userId, request.conversationId());

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);

        // The user id was read above, because the background thread has no login information
        streamExecutor.execute(() -> {
            try {
                chatService.chatStreaming(userId, request.conversationId(), message,
                        id -> send(emitter, "meta", Map.of("conversationId", id)),
                        piece -> send(emitter, "token", Map.of("text", piece)));
                send(emitter, "done", Map.of());
            } catch (Exception e) {
                log.warn("Streaming chat failed: {}", e.getMessage());
                try {
                    send(emitter, "error", Map.of("message", friendlyMessage(e)));
                } catch (RuntimeException ignored) {
                    // the browser is already gone
                }
            } finally {
                emitter.complete();
            }
        });

        return emitter;
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            // Thrown when the browser disconnected: this makes ChatService undo the message
            throw new UncheckedIOException(e);
        }
    }

    private String friendlyMessage(Exception e) {
        if (e instanceof ResponseStatusException rse && rse.getStatusCode().value() == 502) {
            return "The AI service is unavailable right now. Please try again in a moment.";
        }
        return "Something went wrong. Please try again.";
    }

    private String requireValidMessage(ChatRequest request) {
        String message = request.message();
        if (message == null || message.isBlank() || message.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message");
        }
        return message;
    }
}