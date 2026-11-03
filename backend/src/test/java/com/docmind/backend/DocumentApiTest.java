package com.docmind.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real HTTP against the whole app (like ApiIntegrationTest), for the PDF upload endpoints.
 * The test profile limits files to 1 MB and stores them in target/test-uploads.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DocumentApiTest {

    private static final String PASSWORD = "CorrectHorse123";
    private static final byte[] TINY_PDF =
            "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    @Value("${local.server.port}")
    private int port;

    @Value("${app.storage.dir}")
    private String storageDir;

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    // ---------- the happy path ----------

    @Test
    void uploadListAndDeleteWorkAndTheFileOnDiskHasARandomName() throws Exception {
        String token = registerAndLogin();
        Set<String> before = filesOnDisk();

        HttpResponse<String> created = upload(token, "file", "My Secret Report.pdf", TINY_PDF);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat((String) JsonPath.read(created.body(), "$.originalFilename")).isEqualTo("My Secret Report.pdf");
        assertThat(((Number) JsonPath.read(created.body(), "$.sizeBytes")).longValue()).isEqualTo(TINY_PDF.length);
        assertThat(created.body()).doesNotContain("storedFilename");
        long id = ((Number) JsonPath.read(created.body(), "$.id")).longValue();

        // Exactly one new file, named by us (a UUID), not by the person
        Set<String> added = new HashSet<>(filesOnDisk());
        added.removeAll(before);
        assertThat(added).hasSize(1);
        assertThat(added.iterator().next()).matches("[0-9a-f\\-]{36}\\.pdf");

        assertThat(listSize(token)).isEqualTo(1);

        assertThat(send("DELETE", "/api/documents/" + id, token).statusCode()).isEqualTo(204);
        assertThat(listSize(token)).isEqualTo(0);
        assertThat(filesOnDisk()).isEqualTo(before); // the file is gone too
        assertThat(send("DELETE", "/api/documents/" + id, token).statusCode()).isEqualTo(404);
    }

    // ---------- things that must be refused ----------

    @Test
    void aFileThatIsNotReallyAPdfIsRefusedEvenWithAPdfNameAndType() throws Exception {
        String token = registerAndLogin();
        Set<String> before = filesOnDisk();

        HttpResponse<String> response =
                upload(token, "file", "innocent.pdf", "I am a text file".getBytes(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(listSize(token)).isEqualTo(0);
        assertThat(filesOnDisk()).isEqualTo(before); // nothing was stored
    }

    @Test
    void anEmptyFileIsRefused() throws Exception {
        String token = registerAndLogin();

        assertThat(upload(token, "file", "empty.pdf", new byte[0]).statusCode()).isEqualTo(400);
        assertThat(listSize(token)).isEqualTo(0);
    }

    @Test
    void aFileOverTheLimitGets413AndIsNotStored() throws Exception {
        String token = registerAndLogin();
        Set<String> before = filesOnDisk();

        // 1.5 MB with a real PDF start: only the size is wrong (the test limit is 1 MB)
        byte[] big = new byte[1_500_000];
        System.arraycopy(TINY_PDF, 0, big, 0, TINY_PDF.length);

        assertThat(upload(token, "file", "big.pdf", big).statusCode()).isEqualTo(413);
        assertThat(listSize(token)).isEqualTo(0);
        assertThat(filesOnDisk()).isEqualTo(before);
    }

    @Test
    void aRequestWithoutTheFilePartIsRefused() throws Exception {
        String token = registerAndLogin();

        assertThat(upload(token, "wrong-field-name", "a.pdf", TINY_PDF).statusCode()).isEqualTo(400);
    }

    @Test
    void documentEndpointsRequireLogin() throws Exception {
        assertThat(upload(null, "file", "a.pdf", TINY_PDF).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/documents", null).statusCode()).isEqualTo(401);
        assertThat(send("DELETE", "/api/documents/1", null).statusCode()).isEqualTo(401);
    }

    // ---------- the most important security rule, again ----------

    @Test
    void aUserCannotSeeOrDeleteSomeoneElsesDocument() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        Set<String> before = filesOnDisk();

        HttpResponse<String> created = upload(tokenA, "file", "private.pdf", TINY_PDF);
        long id = ((Number) JsonPath.read(created.body(), "$.id")).longValue();

        // B sees nothing and cannot delete. "Not found", so the id is not even confirmed.
        assertThat(listSize(tokenB)).isEqualTo(0);
        assertThat(send("DELETE", "/api/documents/" + id, tokenB).statusCode()).isEqualTo(404);

        // A still has the document and the file
        assertThat(listSize(tokenA)).isEqualTo(1);
        assertThat(filesOnDisk()).hasSize(before.size() + 1);
    }

    // ---------- hostile file names ----------

    @Test
    void aHostileFileNameIsCleanedAndNothingEscapesTheStorageFolder() throws Exception {
        String token = registerAndLogin();

        HttpResponse<String> created = upload(token, "file", "../../evil.pdf", TINY_PDF);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat((String) JsonPath.read(created.body(), "$.originalFilename")).isEqualTo("evil.pdf");

        Path storage = Path.of(storageDir).toAbsolutePath().normalize();
        assertThat(Files.exists(storage.getParent().resolve("evil.pdf"))).isFalse();
        assertThat(Files.exists(storage.resolve("evil.pdf"))).isFalse();
    }

    // ---------- helpers ----------

    private HttpResponse<String> upload(String token, String field, String filename, byte[] content)
            throws Exception {
        String boundary = "----docmind" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + "/api/documents"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return http.send(builder.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(String method, String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return http.send(builder.method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private int listSize(String token) throws Exception {
        Integer size = JsonPath.read(send("GET", "/api/documents", token).body(), "$.length()");
        return size;
    }

    private Set<String> filesOnDisk() throws IOException {
        Path dir = Path.of(storageDir).toAbsolutePath().normalize();
        try (Stream<Path> files = Files.list(dir)) {
            Set<String> names = new HashSet<>();
            files.forEach(p -> names.add(p.getFileName().toString()));
            return names;
        }
    }

    private String registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String registerJson = "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test User\"}"
                .formatted(email, PASSWORD);
        String loginJson = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD);

        assertThat(postJson("/api/auth/register", registerJson).statusCode()).isEqualTo(201);
        HttpResponse<String> login = postJson("/api/auth/login", loginJson);
        assertThat(login.statusCode()).isEqualTo(200);
        return JsonPath.read(login.body(), "$.token");
    }

    private HttpResponse<String> postJson(String path, String json) throws Exception {
        HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}