package ru.skypro.homework.controller.ad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import ru.skypro.homework.AbstractIntegrationTest;
import ru.skypro.homework.dto.ad.AdDto;
import ru.skypro.homework.dto.ad.AdsDto;
import ru.skypro.homework.dto.ad.CreateOrUpdateAdDto;
import ru.skypro.homework.dto.ad.ExtendedAdDto;
import ru.skypro.homework.dto.auth.Role;
import ru.skypro.homework.model.AdsDao;
import ru.skypro.homework.model.UsersDao;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;
import ru.skypro.homework.service.ImageService;

import java.util.UUID;

class AdControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;

    @Autowired private AdRepository adRepository;

    @Autowired private CommentRepository commentRepository;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private ImageService imageService;

    private UsersDao testUser;
    private UsersDao adminUser;
    private AdsDao testAd;

    private final String userEmail = "user@test.com";
    private final String userPassword = "password";
    private final String adminEmail = "admin@test.com";
    private final String adminPassword = "admin";

    @BeforeEach
    void setUp() throws Exception {
        testUser = new UsersDao();
        testUser.setEmail(userEmail);
        testUser.setPassword(passwordEncoder.encode(userPassword));
        testUser.setFirstName("Иван");
        testUser.setLastName("Иванов");
        testUser.setPhone("+7 (999) 123-45-67");
        testUser.setRole(Role.USER);
        userRepository.save(testUser);

        adminUser = new UsersDao();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setFirstName("Пётр");
        adminUser.setLastName("Петров");
        adminUser.setPhone("+7 (999) 765-43-21");
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);

        testAd = new AdsDao();
        testAd.setTitle("Test Ad");
        testAd.setDescription("Test Description");
        testAd.setPrice(1000);
        testAd.setAuthor(testUser);
        testAd.setImage("/ads-images/test.jpg");
        adRepository.save(testAd);

        when(imageService.saveImage(any(MultipartFile.class), anyString(), anyString()))
                .thenAnswer(
                        invocation -> {
                            MultipartFile file = invocation.getArgument(0);
                            String originalFilename = file.getOriginalFilename();
                            String extension = "";
                            if (originalFilename != null && originalFilename.contains(".")) {
                                extension =
                                        originalFilename.substring(
                                                originalFilename.lastIndexOf("."));
                            }
                            return "/ads-images/" + UUID.randomUUID() + extension;
                        });

        when(imageService.readImageAsBytes(anyString(), anyString()))
                .thenAnswer(
                        invocation -> {
                            String imagePath = invocation.getArgument(0);
                            if (imagePath.contains("new")) {
                                return "new image content".getBytes();
                            } else {
                                return "image content".getBytes();
                            }
                        });
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        adRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getAllAds_ShouldReturnList() {
        ResponseEntity<AdsDto> response =
                withAuth(userEmail, userPassword).getForEntity(baseUrl() + "/ads", AdsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCount()).isPositive();
        assertThat(response.getBody().getResults()).isNotEmpty();
    }

    @Test
    void addAd_ShouldCreateAd() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CreateOrUpdateAdDto properties = new CreateOrUpdateAdDto();
        properties.setTitle("New Ad");
        properties.setDescription("New Description");
        properties.setPrice(999);

        String propertiesJson = objectMapper.writeValueAsString(properties);
        ByteArrayResource propertiesPart =
                new ByteArrayResource(propertiesJson.getBytes()) {
                    @Override
                    public String getFilename() {
                        return "properties.json";
                    }
                };

        // Оборачиваем JSON часть в HttpEntity с заголовком Content-Type
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                getMultiValueMapHttpEntity(propertiesPart, jsonHeaders, headers);

        ResponseEntity<AdDto> response =
                withAuth(userEmail, userPassword)
                        .postForEntity(baseUrl() + "/ads", requestEntity, AdDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("New Ad");
        assertThat(response.getBody().getAuthor()).isEqualTo(testUser.getId());

        assertThat(adRepository.findByAuthorId(testUser.getId())).hasSize(2);
    }

    private static @NotNull HttpEntity<MultiValueMap<String, Object>> getMultiValueMapHttpEntity(
            ByteArrayResource propertiesPart, HttpHeaders jsonHeaders, HttpHeaders headers) {
        HttpEntity<ByteArrayResource> jsonEntity = new HttpEntity<>(propertiesPart, jsonHeaders);

        // Для изображения используем просто ByteArrayResource, без обёртки
        ByteArrayResource imagePart =
                new ByteArrayResource("image content".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "image.jpg";
                    }
                };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("properties", jsonEntity); // обёрнутый JSON
        body.add("image", imagePart); // простой ресурс

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return requestEntity;
    }

    @Test
    void getAd_ShouldReturnExtendedAd() {
        ResponseEntity<ExtendedAdDto> response =
                withAuth(userEmail, userPassword)
                        .getForEntity(baseUrl() + "/ads/{id}", ExtendedAdDto.class, testAd.getPk());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPk()).isEqualTo(testAd.getPk());
        assertThat(response.getBody().getAuthorFirstName()).isEqualTo(testUser.getFirstName());
        assertThat(response.getBody().getAuthorLastName()).isEqualTo(testUser.getLastName());
        assertThat(response.getBody().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getBody().getPhone()).isEqualTo(testUser.getPhone());
        assertThat(response.getBody().getTitle()).isEqualTo(testAd.getTitle());
        assertThat(response.getBody().getDescription()).isEqualTo(testAd.getDescription());
        assertThat(response.getBody().getPrice()).isEqualTo(testAd.getPrice());
        assertThat(response.getBody().getImage()).isEqualTo(testAd.getImage());
    }

    @Test
    void removeAd_ByAuthor_ShouldReturnNoContent() {
        ResponseEntity<Void> response =
                withAuth(userEmail, userPassword)
                        .exchange(
                                baseUrl() + "/ads/" + testAd.getPk(),
                                HttpMethod.DELETE,
                                null,
                                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adRepository.findById(testAd.getPk())).isEmpty();
    }

    @Test
    void removeAd_ByAdmin_ShouldReturnNoContent() {
        ResponseEntity<Void> response =
                withAuth(adminEmail, adminPassword)
                        .exchange(
                                baseUrl() + "/ads/" + testAd.getPk(),
                                HttpMethod.DELETE,
                                null,
                                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adRepository.findById(testAd.getPk())).isEmpty();
    }

    @Test
    void removeAd_ByOtherUser_ShouldReturnForbidden() {
        UsersDao other = new UsersDao();
        other.setEmail("other@test.com");
        other.setPassword(passwordEncoder.encode("password"));
        other.setFirstName("Другой");
        other.setLastName("Пользователь");
        other.setPhone("+7 (999) 111-22-33");
        other.setRole(Role.USER);
        userRepository.save(other);

        ResponseEntity<Void> response =
                withAuth("other@test.com", "password")
                        .exchange(
                                baseUrl() + "/ads/" + testAd.getPk(),
                                HttpMethod.DELETE,
                                null,
                                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(adRepository.findById(testAd.getPk())).isPresent();
    }

    @Test
    void updateAd_ByAuthor_ShouldReturnUpdatedAd() {
        CreateOrUpdateAdDto update = new CreateOrUpdateAdDto();
        update.setTitle("Updated Title");
        update.setDescription("Updated Description");
        update.setPrice(2000);

        ResponseEntity<AdDto> response =
                patchWithAuth(
                        baseUrl() + "/ads/" + testAd.getPk(),
                        update,
                        AdDto.class,
                        userEmail,
                        userPassword);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Title");
        assertThat(response.getBody().getPrice()).isEqualTo(2000);

        AdsDao updated = adRepository.findById(testAd.getPk()).orElseThrow();
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void getAdsMe_ShouldReturnUserAds() {
        ResponseEntity<AdsDto> response =
                withAuth(userEmail, userPassword).getForEntity(baseUrl() + "/ads/me", AdsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCount()).isEqualTo(1);
        assertThat(response.getBody().getResults()).hasSize(1);
        assertThat(response.getBody().getResults().get(0).getPk()).isEqualTo(testAd.getPk());
    }

    @Test
    void updateImage_ShouldReplaceImage() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource imagePart =
                new ByteArrayResource("new image content".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "newimage.jpg";
                    }
                };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        ResponseEntity<byte[]> response =
                patchMultipartWithAuth(
                        baseUrl() + "/ads/" + testAd.getPk() + "/image",
                        body,
                        byte[].class,
                        userEmail,
                        userPassword);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AdsDao updated = adRepository.findById(testAd.getPk()).orElseThrow();
        assertThat(updated.getImage()).startsWith("/ads-images/");
        assertThat(updated.getImage()).isNotEqualTo(testAd.getImage());
    }

    // ====== ТЕСТЫ НА 401 (БЕЗ АВТОРИЗАЦИИ) ======

    @Test
    void addAd_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CreateOrUpdateAdDto properties = new CreateOrUpdateAdDto();
        properties.setTitle("New Ad");
        properties.setDescription("New Description");
        properties.setPrice(999);

        String propertiesJson = objectMapper.writeValueAsString(properties);
        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                getMultiValueMapHttpEntity(propertiesJson, headers);

        ResponseEntity<AdDto> response =
                restTemplate.postForEntity(baseUrl() + "/ads", requestEntity, AdDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static @NotNull HttpEntity<MultiValueMap<String, Object>> getMultiValueMapHttpEntity(
            String propertiesJson, HttpHeaders headers) {
        ByteArrayResource propertiesPart =
                new ByteArrayResource(propertiesJson.getBytes()) {
                    @Override
                    public String getFilename() {
                        return "properties.json";
                    }
                };

        ByteArrayResource imagePart =
                new ByteArrayResource("image content".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "image.jpg";
                    }
                };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("properties", propertiesPart);
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return requestEntity;
    }

    @Test
    void updateAd_WithoutAuth_ShouldReturnUnauthorized() {
        CreateOrUpdateAdDto update = new CreateOrUpdateAdDto();
        update.setTitle("Updated Title");
        update.setDescription("Updated Description");
        update.setPrice(2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(
                "wrong", "creds"); // можно не ставить, но для чистоты используем без аутентификации
        HttpEntity<CreateOrUpdateAdDto> requestEntity = new HttpEntity<>(update, headers);

        ResponseEntity<AdDto> response =
                restTemplate.exchange(
                        baseUrl() + "/ads/" + testAd.getPk(),
                        HttpMethod.PATCH,
                        requestEntity,
                        AdDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void removeAd_WithoutAuth_ShouldReturnUnauthorized() {
        ResponseEntity<Void> response =
                restTemplate.exchange(
                        baseUrl() + "/ads/" + testAd.getPk(), HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getAdsMe_WithoutAuth_ShouldReturnUnauthorized() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(baseUrl() + "/ads/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateImage_WithoutAuth_ShouldReturnUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource imagePart =
                new ByteArrayResource("new image".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "image.jpg";
                    }
                };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response =
                restTemplate.exchange(
                        baseUrl() + "/ads/" + testAd.getPk() + "/image",
                        HttpMethod.PATCH,
                        requestEntity,
                        Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ====== ТЕСТЫ на 404 для несуществующего объявления ======

    @Test
    void getAd_NotFound_ShouldReturn404() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(userEmail, userPassword);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        baseUrl() + "/ads/999999", HttpMethod.GET, requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAd_NotFound_ShouldReturn404() {
        CreateOrUpdateAdDto update = new CreateOrUpdateAdDto();
        update.setTitle("Updated Title");
        update.setDescription("Updated Description");
        update.setPrice(2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(userEmail, userPassword);
        HttpEntity<CreateOrUpdateAdDto> requestEntity = new HttpEntity<>(update, headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        baseUrl() + "/ads/999999", HttpMethod.PATCH, requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removeAd_NotFound_ShouldReturn404() {
        ResponseEntity<Void> response =
                withAuth(userEmail, userPassword)
                        .exchange(baseUrl() + "/ads/999999", HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateImage_NotFound_ShouldReturn404() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBasicAuth(userEmail, userPassword);

        ByteArrayResource imagePart =
                new ByteArrayResource("new image".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "image.jpg";
                    }
                };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response =
                restTemplate.exchange(
                        baseUrl() + "/ads/999999/image",
                        HttpMethod.PATCH,
                        requestEntity,
                        Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ====== ТЕСТЫ на 403 (обновление чужого объявления)

    @Test
    void updateAd_ByOtherUser_ShouldReturnForbidden() {
        CreateOrUpdateAdDto update = new CreateOrUpdateAdDto();
        update.setTitle("Hacked Title");
        update.setDescription("Hacked Description");
        update.setPrice(1);

        UsersDao other = new UsersDao();
        other.setEmail("other2@test.com");
        other.setPassword(passwordEncoder.encode("password"));
        other.setFirstName("Другой");
        other.setLastName("Пользователь");
        other.setPhone("+7 (999) 111-22-33");
        other.setRole(Role.USER);
        userRepository.save(other);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("other2@test.com", "password");
        HttpEntity<CreateOrUpdateAdDto> requestEntity = new HttpEntity<>(update, headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        baseUrl() + "/ads/" + testAd.getPk(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
