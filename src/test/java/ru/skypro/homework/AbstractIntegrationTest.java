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

/**
 * Абстрактный базовый класс для интеграционных тестов, обеспечивающий:
 * <ul>
 *   <li>Запуск PostgreSQL в Docker-контейнере с помощью Testcontainers.</li>
 *   <li>Создание временных директорий для хранения тестовых изображений (аватаров и картинок объявлений).</li>
 *   <li>Автоматическую подстановку динамических свойств (URL базы данных, пути к директориям) в контекст Spring.</li>
 *   <li>Вспомогательные методы для выполнения HTTP-запросов с Basic-аутентификацией.</li>
 *   <li>Очистку временных директорий перед каждым тестом.</li>
 * </ul>
 * <p>
 * Все интеграционные тесты контроллеров и репозиториев должны наследовать этот класс,
 * чтобы использовать общую инфраструктуру и избежать дублирования кода.
 *
 * @see org.springframework.boot.test.context.SpringBootTest
 * @see org.testcontainers.containers.PostgreSQLContainer
 * @see org.springframework.test.context.DynamicPropertySource
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    /**
     * Контейнер PostgreSQL, запускаемый один раз для всех тестов.
     * Используется образ postgres:15.
     */
    protected static final PostgreSQLContainer<?> postgres;

    static {
        // Принудительно устанавливаем версию Docker API для стабильной работы с Testcontainers
        System.setProperty("docker.client.version", "1.44");
        // Отключаем Ryuk (контейнер для очистки) в CI-окружении для предотвращения ошибок
        System.setProperty("testcontainers.ryuk.disabled", "true");
        Duration timeout = "true".equals(System.getenv("CI")) ? Duration.ofMinutes(3) : Duration.ofMinutes(1);
        postgres = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withExposedPorts(5432)
                .withStartupTimeout(timeout);
        postgres.start();
        // Гарантированно останавливаем контейнер при завершении JVM
        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
    }

    /** Временная директория для хранения аватаров в тестах. */
    private static Path tempAvatarDir;

    /** Временная директория для хранения изображений объявлений в тестах. */
    private static Path tempAdImageDir;

    /**
     * Создаёт временные директории для изображений перед выполнением всех тестов.
     * Директории будут автоматически удалены при завершении JVM.
     *
     * @throws IOException если не удаётся создать временные директории
     */
    @BeforeAll
    static void createTempDirectories() throws IOException {
        tempAvatarDir = Files.createTempDirectory("test-avatars");
        tempAdImageDir = Files.createTempDirectory("test-ads-images");
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

    /**
     * Динамически добавляет свойства для подключения к тестовой БД и временным директориям.
     * Эти свойства переопределяют значения из {@code application.properties}.
     *
     * @param registry реестр динамических свойств Spring
     */
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

    /** Порт, на котором запущен встроенный Tomcat (назначается случайно). */
    @LocalServerPort
    protected int port;

    /** {@link TestRestTemplate} для выполнения HTTP-запросов в тестах. */
    @Autowired
    protected TestRestTemplate restTemplate;

    /** {@link PasswordEncoder} для шифрования паролей пользователей. */
    @Autowired
    protected PasswordEncoder passwordEncoder;

    /**
     * Возвращает базовый URL тестируемого приложения, включая случайный порт.
     *
     * @return базовый URL, например {@code http://localhost:12345}
     */
    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Возвращает экземпляр {@link TestRestTemplate}, предварительно настроенный
     * с Basic-аутентификацией для указанных учётных данных.
     *
     * @param username имя пользователя (email)
     * @param password пароль
     * @return шаблон с Basic-аутентификацией
     */
    protected TestRestTemplate withAuth(String username, String password) {
        return restTemplate.withBasicAuth(username, password);
    }

    /**
     * Выполняет PATCH-запрос с телом JSON, используя Basic-аутентификацию.
     *
     * @param url           URL запроса (может содержать шаблонные переменные)
     * @param request       объект, который будет сериализован в JSON и передан в теле запроса
     * @param responseType  ожидаемый тип ответа
     * @param username      имя пользователя для аутентификации
     * @param password      пароль
     * @param uriVariables  значения шаблонных переменных URL
     * @param <T>           тип ответа
     * @return              ответ сервера, преобразованный в {@link ResponseEntity}
     */
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

    /**
     * Выполняет PATCH-запрос с multipart/form-data телом, используя Basic-аутентификацию.
     *
     * @param url           URL запроса (может содержать шаблонные переменные)
     * @param body          {@link MultiValueMap}, содержащий части запроса (например, файлы)
     * @param responseType  ожидаемый тип ответа
     * @param username      имя пользователя для аутентификации
     * @param password      пароль
     * @param uriVariables  значения шаблонных переменных URL
     * @param <T>           тип ответа
     * @return              ответ сервера, преобразованный в {@link ResponseEntity}
     */
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

    /**
     * Очищает временные директории с изображениями перед каждым тестом,
     * удаляя все файлы и поддиректории, а затем заново создаёт пустые директории.
     *
     * @throws IOException если возникает ошибка ввода-вывода при очистке
     */
    @BeforeEach
    void cleanImageDirectories() throws IOException {
        cleanDirectory(tempAvatarDir);
        cleanDirectory(tempAdImageDir);
    }

    /**
     * Рекурсивно удаляет все файлы в указанной директории и создаёт её заново.
     *
     * @param dir путь к директории
     * @throws IOException если не удаётся создать директорию после очистки
     */
    private void cleanDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                 .sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (IOException e) {
                         // Игнорируем, так как директория всё равно будет пересоздана
                     }
                 });
        }
        Files.createDirectories(dir);
    }

    /**
     * Возвращает путь к временной директории для хранения аватаров.
     * Используется в тестах для проверки физического сохранения файлов.
     *
     * @return {@link Path} к директории аватаров
     */
    protected Path getAvatarDir() {
        return tempAvatarDir;
    }

    /**
     * Возвращает путь к временной директории для хранения изображений объявлений.
     * Используется в тестах для проверки физического сохранения файлов.
     *
     * @return {@link Path} к директории изображений объявлений
     */
    protected Path getAdImageDir() {
        return tempAdImageDir;
    }
}