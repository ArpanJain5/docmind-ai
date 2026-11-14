package com.docmind.backend.service;

import com.docmind.backend.ai.AiException;
import com.docmind.backend.ai.AiMessage;
import com.docmind.backend.ai.AiService;
import com.docmind.backend.dto.ChatResponse;
import com.docmind.backend.dto.ConversationSummary;
import com.docmind.backend.dto.MessageResponse;
import com.docmind.backend.model.Conversation;
import com.docmind.backend.model.Document;
import com.docmind.backend.model.DocumentStatus;
import com.docmind.backend.model.Message;
import com.docmind.backend.model.Role;
import com.docmind.backend.model.User;
import com.docmind.backend.repository.ConversationRepository;
import com.docmind.backend.repository.DocumentRepository;
import com.docmind.backend.repository.MessageRepository;
import com.docmind.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
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
    private final DocumentRepository documentRepository;
    private final AiService aiService;

    // How much of a document the AI sees with each question
    private final int maxDocumentChars;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       DocumentRepository documentRepository,
                       AiService aiService,
                       @Value("${app.ai.max-document-chars:12000}") int maxDocumentChars) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.aiService = aiService;
        this.maxDocumentChars = maxDocumentChars;
    }

    // ---------- chatting ----------

    // The whole reply at once (an ordinary chat)
    public ChatResponse chat(Long userId, Long conversationId, String userMessage) {
        return chat(userId, conversationId, null, userMessage);
    }

    // The whole reply at once. documentId is only allowed when a NEW chat starts.
    public ChatResponse chat(Long userId, Long conversationId, Long documentId, String userMessage) {
        return converse(userId, conversationId, documentId, userMessage,
                id -> { },
                (prompt, history) -> aiService.complete(prompt, history));
    }

    // The reply piece by piece (an ordinary chat)
    public ChatResponse chatStreaming(Long userId, Long conversationId, String userMessage,
                                      Consumer<Long> onConversation, Consumer<String> onToken) {
        return chatStreaming(userId, conversationId, null, userMessage, onConversation, onToken);
    }

    // The reply piece by piece: onConversation gets the chat id first, then onToken gets each piece
    public ChatResponse chatStreaming(Long userId, Long conversationId, Long documentId, String userMessage,
                                      Consumer<Long> onConversation, Consumer<String> onToken) {
        return converse(userId, conversationId, documentId, userMessage,
                onConversation,
                (prompt, history) -> aiService.stream(prompt, history, onToken));
    }

    // Lets the controller return a real 404 or 400 BEFORE a stream starts
    public void verifyAccess(Long userId, Long conversationId, Long documentId) {
        if (conversationId != null) {
            findOwnedConversation(userId, conversationId);
        }
        resolveDocument(userId, conversationId, documentId);
    }

    @FunctionalInterface
    private interface AiCall {
        String run(String systemPrompt, List<AiMessage> history);
    }

    private ChatResponse converse(Long userId, Long conversationId, Long documentId, String userMessage,
                                  Consumer<Long> onConversation, AiCall aiCall) {
        boolean isNewChat = (conversationId == null);

        // Checked first, so a bad document id saves nothing at all
        Document document = resolveDocument(userId, conversationId, documentId);

        Conversation conversation = isNewChat
                ? conversationRepository.save(
                        new Conversation(findUser(userId), makeTitle(userMessage), document))
                : findOwnedConversation(userId, conversationId);

        Message savedUserMessage =
                messageRepository.save(new Message(conversation, Role.USER, userMessage));

        // The slow part. We deliberately do NOT keep a database transaction open during it.
        String reply;
        try {
            onConversation.accept(conversation.getId());
            String systemPrompt = buildSystemPrompt(userId, conversation);
            reply = callAi(conversation.getId(), systemPrompt, aiCall);
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

    // The document for a NEW chat, or null for an ordinary chat
    private Document resolveDocument(Long userId, Long conversationId, Long documentId) {
        if (documentId == null) {
            return null;
        }
        if (conversationId != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose the document when you start a new chat");
        }

        // Only YOUR documents (404 for anyone else's), same rule as for chats
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        if (document.getStatus() != DocumentStatus.READY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "This document has no readable text");
        }
        return document;
    }

    // The instructions for the AI. For a chat about a document they include the (cut) text.
    private String buildSystemPrompt(Long userId, Conversation conversation) {
        Long documentId = conversation.getDocumentId();
        if (documentId == null) {
            return SYSTEM_PROMPT;
        }
        return documentRepository.findTextByIdAndUserId(documentId, userId)
                .map(text -> PromptBuilder.withDocument(SYSTEM_PROMPT, text, maxDocumentChars))
                .orElse(SYSTEM_PROMPT);
    }

    private String callAi(Long conversationId, String systemPrompt, AiCall aiCall) {
        List<Message> all = messageRepository.findByConversationIdOrderByIdAsc(conversationId);
        List<Message> recent = all.subList(Math.max(0, all.size() - MAX_HISTORY_MESSAGES), all.size());
        List<AiMessage> history = recent.stream()
                .map(m -> new AiMessage(m.getRole(), m.getContent()))
                .toList();

        try {
            return aiCall.run(systemPrompt, history);
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
                .map(c -> ConversationSummary.from(c, maxDocumentChars))
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
        return ConversationSummary.from(conversation, maxDocumentChars);
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