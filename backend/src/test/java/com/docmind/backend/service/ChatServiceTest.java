package com.docmind.backend.service;

import com.docmind.backend.ai.AiException;
import com.docmind.backend.ai.AiMessage;
import com.docmind.backend.ai.AiService;
import com.docmind.backend.dto.ChatResponse;
import com.docmind.backend.model.Conversation;
import com.docmind.backend.model.Message;
import com.docmind.backend.model.Role;
import com.docmind.backend.model.User;
import com.docmind.backend.repository.ConversationRepository;
import com.docmind.backend.repository.MessageRepository;
import com.docmind.backend.repository.UserRepository;
import com.docmind.backend.model.Document;
import com.docmind.backend.model.DocumentStatus;
import com.docmind.backend.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the chat RULES with a pretend database and a pretend AI.
 * No Spring, no MySQL, no internet: these run in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long NEW_CONVERSATION_ID = 5L;

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiService aiService;
    @Mock
    private DocumentRepository documentRepository;

    private ChatService chatService;

    // A tiny pretend database for messages
    private final List<Message> savedMessages = new ArrayList<>();
    private long nextMessageId = 100;

    @BeforeEach
    void setUp() {
            chatService = new ChatService(
            conversationRepository, messageRepository, userRepository,
            documentRepository, aiService, 12_000);

        User user = new User("a@example.com", "hash", "Arpan");
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // Saving a new conversation "gives" it id 5
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            conversation.setId(NEW_CONVERSATION_ID);
            return conversation;
        });

        // Saving a message gives it the next id and remembers it
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(nextMessageId++);
            savedMessages.add(message);
            return message;
        });

        // Reading messages returns whatever was saved so far
        when(messageRepository.findByConversationIdOrderByIdAsc(anyLong()))
                .thenAnswer(invocation -> new ArrayList<>(savedMessages));

        when(aiService.complete(anyString(), anyList())).thenReturn("ok");
    }

    private static int statusOf(Throwable error) {
        assertThat(error).isInstanceOf(ResponseStatusException.class);
        return ((ResponseStatusException) error).getStatusCode().value();
    }

    // ---------- ownership ----------

    @Test
    void readingSomeoneElsesConversationIsNotFound() {
        when(conversationRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> chatService.getMessages(USER_ID, 7L));

        assertThat(statusOf(error)).isEqualTo(404);
        verify(messageRepository, never()).findByConversationIdOrderByIdAsc(anyLong());
    }

    @Test
    void deletingSomeoneElsesConversationIsNotFoundAndDeletesNothing() {
        when(conversationRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> chatService.deleteConversation(USER_ID, 7L));

        assertThat(statusOf(error)).isEqualTo(404);
        verify(messageRepository, never()).deleteByConversationId(anyLong());
        verify(conversationRepository, never()).delete(any(Conversation.class));
    }

    // ---------- normal chatting ----------

    @Test
    void newChatSavesTheQuestionAndTheReply() {
        when(aiService.complete(anyString(), anyList())).thenReturn("Hi there");

        ChatResponse response = chatService.chat(USER_ID, null, "Hello");

        assertThat(response.reply()).isEqualTo("Hi there");
        assertThat(response.conversationId()).isEqualTo(NEW_CONVERSATION_ID);
        assertThat(savedMessages).extracting(Message::getRole)
                .containsExactly(Role.USER, Role.ASSISTANT);
        assertThat(savedMessages).extracting(Message::getContent)
                .containsExactly("Hello", "Hi there");
    }

    @Test
    void longFirstMessagesBecomeAShortTitle() {
        chatService.chat(USER_ID, null, "x".repeat(60));

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("x".repeat(40) + "...");
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyTheLastTwentyMessagesAreSentToTheAi() {
        Conversation existing = new Conversation(null, "Long chat");
        existing.setId(9L);
        when(conversationRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.of(existing));

        for (int i = 0; i < 30; i++) {
            Message old = new Message(existing, i % 2 == 0 ? Role.USER : Role.ASSISTANT, "m" + i);
            old.setId((long) i);
            savedMessages.add(old);
        }

        chatService.chat(USER_ID, 9L, "latest");

        ArgumentCaptor<List<AiMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiService).complete(anyString(), captor.capture());
        List<AiMessage> sent = captor.getValue();
        assertThat(sent).hasSize(20);
        assertThat(sent.get(0).content()).isEqualTo("m11");
        assertThat(sent.get(19).content()).isEqualTo("latest");
    }

    // ---------- when the AI fails, nothing is left behind ----------

    @Test
    void aiFailureUndoesTheQuestionAndTheNewChat() {
        when(aiService.complete(anyString(), anyList())).thenThrow(new AiException("provider is down"));

        Throwable error = catchThrowable(() -> chatService.chat(USER_ID, null, "Hello"));

        assertThat(statusOf(error)).isEqualTo(502);
        verify(messageRepository).deleteById(100L);
        verify(conversationRepository).deleteById(NEW_CONVERSATION_ID);
        // Only the question was ever saved. No reply was stored.
        assertThat(savedMessages).extracting(Message::getRole).containsExactly(Role.USER);
    }

    @Test
    void aiFailureInAnExistingChatRemovesOnlyTheNewQuestion() {
        Conversation existing = new Conversation(null, "Old chat");
        existing.setId(9L);
        when(conversationRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.of(existing));
        when(aiService.complete(anyString(), anyList())).thenThrow(new AiException("provider is down"));

        Throwable error = catchThrowable(() -> chatService.chat(USER_ID, 9L, "Hello again"));

        assertThat(statusOf(error)).isEqualTo(502);
        verify(messageRepository).deleteById(100L);
        verify(conversationRepository, never()).deleteById(anyLong());
    }

    // ---------- streaming ----------

    @Test
    void streamingSendsTheChatIdThenEachPieceAndSavesTheFullReply() {
        when(aiService.stream(anyString(), anyList(), any())).thenAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(2);
            onToken.accept("Hello ");
            onToken.accept("world");
            return "Hello world";
        });

        List<Long> conversationIds = new ArrayList<>();
        List<String> pieces = new ArrayList<>();

        ChatResponse response = chatService.chatStreaming(
                USER_ID, null, "Hi", conversationIds::add, pieces::add);

        assertThat(conversationIds).containsExactly(NEW_CONVERSATION_ID);
        assertThat(pieces).containsExactly("Hello ", "world");
        assertThat(response.reply()).isEqualTo("Hello world");
        assertThat(savedMessages).extracting(Message::getContent)
                .containsExactly("Hi", "Hello world");
    }

    @Test
    void aBrowserThatLeavesMidStreamUndoesTheWholeExchange() {
        when(aiService.stream(anyString(), anyList(), any())).thenAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(2);
            onToken.accept("Hello "); // this throws, like a closed browser connection
            return "never reached";
        });

        Throwable error = catchThrowable(() -> chatService.chatStreaming(
                USER_ID, null, "Hi",
                id -> { },
                piece -> {
                    throw new UncheckedIOException(new IOException("browser left"));
                }));

        assertThat(error).isInstanceOf(UncheckedIOException.class);
        verify(messageRepository).deleteById(100L);
        verify(conversationRepository).deleteById(NEW_CONVERSATION_ID);
        assertThat(savedMessages).extracting(Message::getRole).containsExactly(Role.USER);
    }

        // ---------- chatting about a document ----------

    private Document readableDocument(long id) {
        Document document = new Document(null, "report.pdf", "stored.pdf", 10);
        document.setId(id);
        document.setStatus(DocumentStatus.READY);
        return document;
    }

    @Test
    void aNewChatAboutADocumentSendsTheDocumentTextToTheAi() {
        when(documentRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(readableDocument(3L)));
        when(documentRepository.findTextByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of("The sky is green."));

        chatService.chat(USER_ID, null, 3L, "What colour is the sky?");

        ArgumentCaptor<Conversation> saved = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(saved.capture());
        assertThat(saved.getValue().getDocumentId()).isEqualTo(3L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiService).complete(prompt.capture(), anyList());
        assertThat(prompt.getValue()).contains("<document>").contains("The sky is green.");
    }

    @Test
    void aChatThatAlreadyHasADocumentKeepsSendingIt() {
        Conversation existing = new Conversation(null, "About a document", readableDocument(3L));
        existing.setId(9L);
        when(conversationRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.of(existing));
        when(documentRepository.findTextByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of("The sky is green."));

        chatService.chat(USER_ID, 9L, "And the grass?");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiService).complete(prompt.capture(), anyList());
        assertThat(prompt.getValue()).contains("The sky is green.");
    }

    @Test
    void aChatWhoseDocumentWasDeletedIsAnOrdinaryChatAgain() {
        Conversation existing = new Conversation(null, "Old chat"); // no document
        existing.setId(9L);
        when(conversationRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.of(existing));

        chatService.chat(USER_ID, 9L, "Hello");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiService).complete(prompt.capture(), anyList());
        assertThat(prompt.getValue()).doesNotContain("<document>");
        verify(documentRepository, never()).findTextByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void anotherPersonsDocumentIsNotFoundAndNothingIsSaved() {
        when(documentRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> chatService.chat(USER_ID, null, 3L, "Hello"));

        assertThat(statusOf(error)).isEqualTo(404);
        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void aDocumentWithoutTextCannotBeAttached() {
        Document scan = readableDocument(4L);
        scan.setStatus(DocumentStatus.NO_TEXT);
        when(documentRepository.findByIdAndUserId(4L, USER_ID)).thenReturn(Optional.of(scan));

        Throwable error = catchThrowable(() -> chatService.chat(USER_ID, null, 4L, "Hello"));

        assertThat(statusOf(error)).isEqualTo(400);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void aDocumentCanOnlyBeChosenWhenStartingANewChat() {
        Throwable error = catchThrowable(() -> chatService.chat(USER_ID, 9L, 3L, "Hello"));

        assertThat(statusOf(error)).isEqualTo(400);
        verify(messageRepository, never()).save(any(Message.class));
    }
}