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
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq")
public class GroqAiService implements AiService {

    private static final Logger log =
            LoggerFactory.getLogger(GroqAiService.class);

    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final String model;
    private final int maxTokens;

    public GroqAiService(
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${app.ai.max-tokens:1024}") int maxTokens,
            @Value("${app.ai.timeout-seconds:60}") long timeoutSeconds) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.ai.api-key is missing");
        }

        this.model = model;
        this.maxTokens = maxTokens;

        // Never wait forever for someone else's server
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", "")) // no trailing slash
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    // ---------------------------------------------------------------
    // One answer, all at once
    // ---------------------------------------------------------------
    @Override
    public String complete(String systemPrompt, List<AiMessage> history) {

        Map<String, Object> body = buildBody(systemPrompt, history, false);

        ChatCompletionResponse response;

        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

        } catch (RestClientResponseException e) {
            // The provider answered with an error (401 bad key, 404 bad model, 429 rate limit...)
            log.warn("AI provider returned HTTP {}: {}",
                    e.getStatusCode().value(),
                    shorten(e.getResponseBodyAsString()));
            throw new AiException(
                    "AI provider returned HTTP " + e.getStatusCode().value(), e);

        } catch (ResourceAccessException e) {
            // Network problem or timeout
            log.warn("Could not reach the AI provider: {}", e.getMessage());
            throw new AiException("Could not reach the AI provider", e);

        } catch (RestClientException e) {
            log.warn("Unexpected AI response: {}", e.getMessage());
            throw new AiException("Unexpected AI response", e);
        }

        String text = extractText(response);

        if (text.isBlank()) {
            throw new AiException("The AI returned an empty reply");
        }

        return text;
    }

    // ---------------------------------------------------------------
    // One answer, piece by piece
    // ---------------------------------------------------------------
    @Override
    public String stream(String systemPrompt, List<AiMessage> history, Consumer<String> onToken) {

        Map<String, Object> body = buildBody(systemPrompt, history, true);
        StringBuilder fullText = new StringBuilder();

        try {
            restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            String errorBody = new String(
                                    response.getBody().readNBytes(2000), StandardCharsets.UTF_8);
                            log.warn("AI provider returned HTTP {}: {}",
                                    response.getStatusCode().value(), shorten(errorBody));
                            throw new AiException(
                                    "AI provider returned HTTP " + response.getStatusCode().value());
                        }
                        readStream(response.getBody(), fullText, onToken);
                        return null;
                    });

        } catch (ResourceAccessException e) {
            log.warn("Could not reach the AI provider: {}", e.getMessage());
            throw new AiException("Could not reach the AI provider", e);

        } catch (RestClientException e) {
            log.warn("Unexpected AI response: {}", e.getMessage());
            throw new AiException("Unexpected AI response", e);
        }

        String text = fullText.toString().strip();

        if (text.isBlank()) {
            throw new AiException("The AI returned an empty reply");
        }

        return text;
    }

    // Reads lines like  data: {"choices":[{"delta":{"content":"Hel"}}]}
    // and ends at  data: [DONE]
    private void readStream(InputStream in, StringBuilder fullText, Consumer<String> onToken)
            throws IOException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data:")) {
                continue; // blank separator lines and comments
            }

            String payload = line.substring(5).strip();
            if (payload.equals("[DONE]")) {
                break;
            }
            if (payload.isEmpty()) {
                continue;
            }

            String piece = extractDelta(payload);
            // Reasoning models send "thinking" chunks with no content: we skip those
            if (piece != null && !piece.isEmpty()) {
                fullText.append(piece);
                onToken.accept(piece);
            }
        }
    }

    private String extractDelta(String json) {
        try {
            StreamChunk chunk = jsonMapper.readValue(json, StreamChunk.class);
            if (chunk == null || chunk.choices() == null || chunk.choices().isEmpty()) {
                return null;
            }
            StreamDelta delta = chunk.choices().get(0).delta();
            return delta == null ? null : delta.content();
        } catch (RuntimeException e) {
            throw new AiException("Unexpected AI stream data", e);
        }
    }

    // ---------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------
    private Map<String, Object> buildBody(String systemPrompt, List<AiMessage> history, boolean stream) {
        List<Map<String, String>> messages = toApiMessages(systemPrompt, history);

        // The first entry is the system prompt, so we need at least one more
        if (messages.size() <= 1) {
            throw new AiException("There is no user message to send");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    private List<Map<String, String>> toApiMessages(
            String systemPrompt, List<AiMessage> history) {

        List<Map<String, String>> messages = new ArrayList<>();

        // OpenAI-compatible APIs accept the instructions as a "system" message
        messages.add(Map.of(
                "role", "system",
                "content", systemPrompt == null ? "" : systemPrompt));

        // Keep the saved order exactly as it is
        for (AiMessage message : history) {
            String role = message.role() == Role.USER ? "user" : "assistant";
            messages.add(Map.of(
                    "role", role,
                    "content", message.content()));
        }

        return messages;
    }

    private String extractText(ChatCompletionResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()) {
            return "";
        }

        Choice choice = response.choices().get(0);

        if (choice.message() == null || choice.message().content() == null) {
            return "";
        }

        return choice.message().content().strip();
    }

    private String shorten(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }

    // The slice of the provider's JSON that we care about
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ReplyMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReplyMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StreamChunk(List<StreamChoice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StreamChoice(StreamDelta delta) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StreamDelta(String content) {
    }
}