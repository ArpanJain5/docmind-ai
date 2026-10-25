package com.docmind.backend.service;

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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public ChatResponse chat(Long userId, Long conversationId, String userMessage) {
        Conversation conversation = (conversationId == null)
                ? conversationRepository.save(
                        new Conversation(findUser(userId), makeTitle(userMessage)))
                : findOwnedConversation(userId, conversationId);

        messageRepository.save(new Message(conversation, Role.USER, userMessage));

        // The slow part (later: the real AI call). We deliberately do NOT keep a
        // database transaction open while waiting for it.
        String reply = generateReply(userMessage);

        messageRepository.save(new Message(conversation, Role.ASSISTANT, reply));

        return new ChatResponse(conversation.getId(), reply);
    }

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

    private String generateReply(String userMessage) {
        // Pretend the AI is thinking (real AI calls take 1-5 seconds).
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "You said: \"" + userMessage + "\". (Testing Reply!)";
    }
}