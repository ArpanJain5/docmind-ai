package com.docmind.backend.ai;

import com.docmind.backend.model.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq")
public class GeminiAiService implements AiService {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiAiService.class);

    private static final String BASE_URL =
            "https://api.groq.com/openai/v1";

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;

    public GeminiAiService(
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.max-tokens:1024}") int maxTokens,
            @Value("${app.ai.timeout-seconds:60}") long timeoutSeconds) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "app.ai.api-key is missing");
        }

        this.model = model;
        this.maxTokens = maxTokens;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeader(
                        "Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public String complete(
            String systemPrompt,
            List<AiMessage> history) {

        List<Map<String, String>> messages =
                toApiMessages(systemPrompt, history);

        if (messages.size() <= 1) {
            throw new AiException(
                    "There is no user message to send");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);

        ChatCompletionResponse response;

        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

        } catch (RestClientResponseException e) {
            log.warn("Groq API returned HTTP {}: {}",
                    e.getStatusCode().value(),
                    shorten(e.getResponseBodyAsString()));

            throw new AiException(
                    "AI provider returned HTTP "
                            + e.getStatusCode().value(), e);

        } catch (ResourceAccessException e) {
            log.warn("Could not reach Groq API: {}",
                    e.getMessage());

            throw new AiException(
                    "Could not reach the AI provider", e);

        } catch (RestClientException e) {
            log.warn("Unexpected Groq API response: {}",
                    e.getMessage());

            throw new AiException(
                    "Unexpected AI response", e);
        }

        String text = extractText(response);

        if (text.isBlank()) {
            throw new AiException(
                    "The AI returned an empty reply");
        }

        return text;
    }

    private List<Map<String, String>> toApiMessages(
            String systemPrompt,
            List<AiMessage> history) {

        List<Map<String, String>> messages =
                new ArrayList<>();

        // OpenAI-compatible APIs support a system message.
        messages.add(Map.of(
                "role", "system",
                "content",
                systemPrompt == null ? "" : systemPrompt));

        for (AiMessage message : history) {
            String role = message.role() == Role.USER
                    ? "user"
                    : "assistant";

            // Keep the original message sequence. Do not merge
            // consecutive messages or discard earlier assistant replies.
            messages.add(Map.of(
                    "role", role,
                    "content", message.content()));
        }

        return messages;
    }

    private String extractText(
            ChatCompletionResponse response) {

        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()) {
            return "";
        }

        Choice choice = response.choices().get(0);

        if (choice.message() == null
                || choice.message().content() == null) {
            return "";
        }

        return choice.message().content().strip();
    }

    private String shorten(String text) {
        if (text == null) {
            return "";
        }

        return text.length() <= 300
                ? text
                : text.substring(0, 300) + "...";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }
}