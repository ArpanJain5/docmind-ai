package com.docmind.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the whole app on a random port with an in-memory database and the fake AI,
 * then talks to it over real HTTP, exactly like a browser would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String PASSWORD = "CorrectHorse123";

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    // ---------- accounts and login ----------

    @Test
    void registrationCreatesAnAccountWithoutLeakingPasswords() throws Exception {
        String email = uniqueEmail();

        HttpResponse<String> response = register(email);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat((String) JsonPath.read(response.body(), "$.email")).isEqualTo(email);
        assertThat(response.body()).doesNotContainIgnoringCase("password").doesNotContain("$2a$");
    }

    @Test
    void duplicateEmailsAreRejectedEvenWithDifferentCapitals() throws Exception {
        String email = uniqueEmail();

        assertThat(register(email).statusCode()).isEqualTo(201);
        assertThat(register(email.toUpperCase()).statusCode()).isEqualTo(409);
    }

    @Test
    void invalidRegistrationsReturn400NotForbidden() throws Exception {
        // These would turn into 403 if the /error path were locked (the Day 5 bug)
        assertThat(send("POST", "/api/auth/register", null,
                registerJson("not-an-email", PASSWORD)).statusCode()).isEqualTo(400);
        assertThat(send("POST", "/api/auth/register", null,
                registerJson(uniqueEmail(), "short")).statusCode()).isEqualTo(400);
    }

    @Test
    void wrongPasswordAndUnknownEmailBothGet401() throws Exception {
        String email = uniqueEmail();
        register(email);

        assertThat(login(email, "WrongPassword1").statusCode()).isEqualTo(401);
        assertThat(login(uniqueEmail(), PASSWORD).statusCode()).isEqualTo(401);
    }

    @Test
    void meNeedsAValidToken() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = tokenFrom(login(email, PASSWORD));

        assertThat(send("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/auth/me", "not-a-real-token", null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/auth/me", tamper(token), null).statusCode()).isEqualTo(401);

        HttpResponse<String> me = send("GET", "/api/auth/me", token, null);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(me.body(), "$.email")).isEqualTo(email);
    }

    // ---------- chatting needs a login ----------

    @Test
    void chatAndConversationsRequireLogin() throws Exception {
        String message = chatJson(null, "hello");

        assertThat(send("POST", "/api/chat", null, message).statusCode()).isEqualTo(401);
        assertThat(send("POST", "/api/chat/stream", null, message).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/conversations", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void blankMessagesAreRejected() throws Exception {
        String token = registerAndLogin();

        assertThat(send("POST", "/api/chat", token, chatJson(null, "   ")).statusCode()).isEqualTo(400);
    }

    // ---------- the most important security rule ----------

    @Test
    void aUserCannotReadOrChangeSomeoneElsesChat() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();

        HttpResponse<String> created = send("POST", "/api/chat", tokenA, chatJson(null, "Private note"));
        assertThat(created.statusCode()).isEqualTo(200);
        long id = ((Number) JsonPath.read(created.body(), "$.conversationId")).longValue();
        String messagesPath = "/api/conversations/" + id + "/messages";

        // B is refused everything, and gets "not found" so the id is not even confirmed
        assertThat(send("GET", messagesPath, tokenB, null).statusCode()).isEqualTo(404);
        assertThat(send("PATCH", "/api/conversations/" + id, tokenB, "{\"title\":\"Hacked\"}").statusCode())
                .isEqualTo(404);
        assertThat(send("DELETE", "/api/conversations/" + id, tokenB, null).statusCode()).isEqualTo(404);
        assertThat(send("POST", "/api/chat", tokenB, chatJson(id, "Intruder")).statusCode()).isEqualTo(404);
        assertThat(send("POST", "/api/chat/stream", tokenB, chatJson(id, "Intruder")).statusCode())
                .isEqualTo(404);

        // A still has everything, untouched
        HttpResponse<String> messages = send("GET", messagesPath, tokenA, null);
        assertThat(messages.statusCode()).isEqualTo(200);
        Integer count = JsonPath.read(messages.body(), "$.length()");
        assertThat(count).isEqualTo(2); // the question and the reply
        assertThat(messages.body()).doesNotContain("Intruder").doesNotContain("Hacked");

        // Each user's list contains only their own chats
        assertThat(listSize(tokenA)).isEqualTo(1);
        assertThat(listSize(tokenB)).isEqualTo(0);
    }

    @Test
    void theOwnerCanRenameAndDeleteTheirChat() throws Exception {
        String token = registerAndLogin();
        HttpResponse<String> created = send("POST", "/api/chat", token, chatJson(null, "To be renamed"));
        long id = ((Number) JsonPath.read(created.body(), "$.conversationId")).longValue();

        HttpResponse<String> renamed = send("PATCH", "/api/conversations/" + id, token, "{\"title\":\"New name\"}");
        assertThat(renamed.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(renamed.body(), "$.title")).isEqualTo("New name");

        assertThat(send("PATCH", "/api/conversations/" + id, token, "{\"title\":\"   \"}").statusCode())
                .isEqualTo(400);

        assertThat(send("DELETE", "/api/conversations/" + id, token, null).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/api/conversations/" + id + "/messages", token, null).statusCode())
                .isEqualTo(404);
        assertThat(listSize(token)).isEqualTo(0);
    }

    // ---------- streaming ----------

    @Test
    void streamingSendsMetaThenTokensThenDoneAndSavesTheReply() throws Exception {
        String token = registerAndLogin();

        HttpResponse<String> response = send("POST", "/api/chat/stream", token, chatJson(null, "Stream me"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("text/event-stream");

        String body = response.body();
        int meta = body.indexOf("event:meta");
        int firstToken = body.indexOf("event:token");
        int done = body.indexOf("event:done");
        assertThat(meta).isGreaterThanOrEqualTo(0);
        assertThat(firstToken).isGreaterThan(meta);
        assertThat(done).isGreaterThan(firstToken);
        assertThat(body).doesNotContain("event:error");

        // The finished reply was saved: the question and the answer
        Matcher matcher = Pattern.compile("event:meta\\Rdata:\\{\"conversationId\":(\\d+)\\}").matcher(body);
        assertThat(matcher.find()).isTrue();
        long id = Long.parseLong(matcher.group(1));
        Integer count = JsonPath.read(
                send("GET", "/api/conversations/" + id + "/messages", token, null).body(), "$.length()");
        assertThat(count).isEqualTo(2);
    }

    // ---------- helpers ----------

    private HttpResponse<String> send(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30));

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
        if (json != null) {
            builder.header("Content-Type", "application/json");
            body = HttpRequest.BodyPublishers.ofString(json);
        }

        return http.send(builder.method(method, body).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> register(String email) throws Exception {
        return send("POST", "/api/auth/register", null, registerJson(email, PASSWORD));
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        return send("POST", "/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        assertThat(register(email).statusCode()).isEqualTo(201);
        return tokenFrom(login(email, PASSWORD));
    }

    private static String tokenFrom(HttpResponse<String> loginResponse) {
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        return JsonPath.read(loginResponse.body(), "$.token");
    }

    private int listSize(String token) throws Exception {
        Integer size = JsonPath.read(send("GET", "/api/conversations", token, null).body(), "$.length()");
        return size;
    }

    private static String registerJson(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test User\"}"
                .formatted(email, password);
    }

    private static String chatJson(Long conversationId, String message) {
        return "{\"conversationId\":%s,\"message\":\"%s\"}"
                .formatted(conversationId == null ? "null" : conversationId, message);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    // Damage the signature part of the token (its first character always changes the bytes)
    private static String tamper(String token) {
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? "B" : "A") + parts[2].substring(1);
        return String.join(".", parts);
    }
}