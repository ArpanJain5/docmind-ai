package com.docmind.backend.service;

import com.docmind.backend.ai.AiException;
import com.docmind.backend.ai.AiMessage;
import com.docmind.backend.ai.AiService;
import com.docmind.backend.dto.ChatResponse;
import com.docmind.backend.dto.ConversationSummary;
import com.docmind.backend.dto.MessageResponse;
import com.docmind.backend.model.Conversation;
import com.docmind.backend.model.Message;
import com.docmind.backend.model.Role;
import com.docmind.backend.model.User;
import com.docmind.backend.repository.ConversationRepository;
import com.docmind.backend.repository.MessageRepository;
import com.docmind.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.function.Consumer;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT =
            "You are DocMind, a helpful assistant inside the DocMind AI workspace. "
            + "Answer clearly and concisely. If you are not sure about something, say so "
            + "instead of guessing.";

    // The AI has no memory, so we resend recent messages. A cap keeps cost predictable.
    private static final int MAX_HISTORY_MESSAGES = 20;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiService aiService;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       AiService aiService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
    }

    // ---------- chatting ----------

    // The whole reply at once
    public ChatResponse chat(Long userId, Long conversationId, String userMessage) {
        return converse(userId, conversationId, userMessage,
                id -> { },
                history -> aiService.complete(SYSTEM_PROMPT, history));
    }

    // The reply piece by piece: onConversation gets the chat id first, then onToken gets each piece
    public ChatResponse chatStreaming(Long userId, Long conversationId, String userMessage,
                                      Consumer<Long> onConversation, Consumer<String> onToken) {
        return converse(userId, conversationId, userMessage,
                onConversation,
                history -> aiService.stream(SYSTEM_PROMPT, history, onToken));
    }

    // Lets the controller return a real 404 BEFORE a stream starts
    public void verifyAccess(Long userId, Long conversationId) {
        if (conversationId != null) {
            findOwnedConversation(userId, conversationId);
        }
    }

    @FunctionalInterface
    private interface AiCall {
        String run(List<AiMessage> history);
    }

    private ChatResponse converse(Long userId, Long conversationId, String userMessage,
                                  Consumer<Long> onConversation, AiCall aiCall) {
        boolean isNewChat = (conversationId == null);

        Conversation conversation = isNewChat
                ? conversationRepository.save(
                        new Conversation(findUser(userId), makeTitle(userMessage)))
                : findOwnedConversation(userId, conversationId);

        Message savedUserMessage =
                messageRepository.save(new Message(conversation, Role.USER, userMessage));

        // The slow part. We deliberately do NOT keep a database transaction open during it.
        String reply;
        try {
            onConversation.accept(conversation.getId());
            reply = callAi(conversation.getId(), aiCall);
        } catch (RuntimeException e) {
            // The AI failed, or the browser stopped listening: undo what we saved
            messageRepository.deleteById(savedUserMessage.getId());
            if (isNewChat) {
                conversationRepository.deleteById(conversation.getId());
            }
            throw e;
        }

        messageRepository.save(new Message(conversation, Role.ASSISTANT, reply));

        return new ChatResponse(conversation.getId(), reply);
    }

    private String callAi(Long conversationId, AiCall aiCall) {
        List<Message> all = messageRepository.findByConversationIdOrderByIdAsc(conversationId);
        List<Message> recent = all.subList(Math.max(0, all.size() - MAX_HISTORY_MESSAGES), all.size());
        List<AiMessage> history = recent.stream()
                .map(m -> new AiMessage(m.getRole(), m.getContent()))
                .toList();

        try {
            return aiCall.run(history);
        } catch (AiException e) {
            // 502 = "the service behind me failed". The details stay in our logs.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "The AI service is unavailable", e);
        }
    }

    // ---------- chats list, messages, rename, delete ----------

    public List<ConversationSummary> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(ConversationSummary::from)
                .toList();
    }

    public List<MessageResponse> getMessages(Long userId, Long conversationId) {
        findOwnedConversation(userId, conversationId); // 404 unless it is YOURS
        return messageRepository.findByConversationIdOrderByIdAsc(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    // Messages first, then the conversation, all or nothing
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = findOwnedConversation(userId, conversationId);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ConversationSummary renameConversation(Long userId, Long conversationId, String title) {
        Conversation conversation = findOwnedConversation(userId, conversationId);
        conversation.setTitle(title.strip());
        return ConversationSummary.from(conversation);
    }

    // ---------- helpers ----------

    // The one place that decides "is this chat yours?"
    private Conversation findOwnedConversation(Long userId, Long conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }

    private String makeTitle(String firstMessage) {
        String clean = firstMessage.strip();
        return clean.length() <= 40 ? clean : clean.substring(0, 40) + "...";
    }
}