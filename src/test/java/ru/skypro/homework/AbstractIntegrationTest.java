package ru.skypro.homework;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres;

    static {
        System.setProperty("docker.client.version", "1.44");
        System.setProperty("testcontainers.ryuk.disabled", "true");
        Duration timeout = "true".equals(System.getenv("CI")) ? Duration.ofMinutes(3) : Duration.ofMinutes(1);
        postgres = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withExposedPorts(5432)
                .withStartupTimeout(timeout);
        postgres.start();
        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
    }

    private static Path tempAvatarDir;
    private static Path tempAdImageDir;

    @BeforeAll
    static void createTempDirectories() throws IOException {
        tempAvatarDir = Files.createTempDirectory("test-avatars");
        tempAdImageDir = Files.createTempDirectory("test-ads-images");
        // Удаление при завершении JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (tempAvatarDir != null) {
                    Files.walk(tempAvatarDir)
                         .sorted(Comparator.reverseOrder())
                         .forEach(path -> {
                             try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                         });
                }
                if (tempAdImageDir != null) {
                    Files.walk(tempAdImageDir)
                         .sorted(Comparator.reverseOrder())
                         .forEach(path -> {
                             try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                         });
                }
            } catch (IOException ignored) {}
        }));
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.image.avatar-dir", tempAvatarDir::toString);
        registry.add("app.image.ad-dir", tempAdImageDir::toString);
        registry.add("spring.datasource.hikari.connection-timeout", () -> "60000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "60000");
    }

    @LocalServerPort protected int port;

    @Autowired protected TestRestTemplate restTemplate;

    @Autowired protected PasswordEncoder passwordEncoder;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected TestRestTemplate withAuth(String username, String password) {
        return restTemplate.withBasicAuth(username, password);
    }

    protected <T> ResponseEntity<T> patchWithAuth(
            String url,
            Object request,
            Class<T> responseType,
            String username,
            String password,
            Object... uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        HttpEntity<?> entity = new HttpEntity<>(request, headers);
        return restTemplate.exchange(url, HttpMethod.PATCH, entity, responseType, uriVariables);
    }

    protected <T> ResponseEntity<T> patchMultipartWithAuth(
            String url,
            MultiValueMap<String, Object> body,
            Class<T> responseType,
            String username,
            String password,
            Object... uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.PATCH, entity, responseType, uriVariables);
    }

    @BeforeEach
    void cleanImageDirectories() throws IOException {
        cleanDirectory(tempAvatarDir);
        cleanDirectory(tempAdImageDir);
    }

    private void cleanDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                 .sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (IOException e) {
                         // ignore
                     }
                 });
        }
        Files.createDirectories(dir);
    }

    protected Path getAvatarDir() {
        return tempAvatarDir;
    }

    protected Path getAdImageDir() {
        return tempAdImageDir;
    }
}