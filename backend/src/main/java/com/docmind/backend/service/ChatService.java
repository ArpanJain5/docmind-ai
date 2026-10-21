package com.docmind.backend.service;

import com.docmind.backend.dto.ChatResponse;
import com.docmind.backend.dto.MessageResponse;
import com.docmind.backend.model.Conversation;
import com.docmind.backend.model.Message;
import com.docmind.backend.model.Role;
import com.docmind.backend.repository.ConversationRepository;
import com.docmind.backend.repository.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public ChatResponse chat(Long conversationId, String userMessage) {
        Conversation conversation = (conversationId == null)
                ? conversationRepository.save(new Conversation(makeTitle(userMessage)))
                : findConversation(conversationId);

        messageRepository.save(new Message(conversation, Role.USER, userMessage));

        // The slow part (later: the real AI call). We deliberately do NOT keep a
        // database transaction open while waiting for it.
        String reply = generateReply(userMessage);

        messageRepository.save(new Message(conversation, Role.ASSISTANT, reply));

        return new ChatResponse(conversation.getId(), reply);
    }

    public List<MessageResponse> getMessages(Long conversationId) {
        findConversation(conversationId); // throws 404 if it does not exist
        return messageRepository.findByConversationIdOrderByIdAsc(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    private Conversation findConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conversation not found"));
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