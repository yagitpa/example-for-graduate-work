package ru.skypro.homework.controller.ad;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class AdControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private AdRepository adRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ObjectMapper objectMapper;

    @Value("${app.image.ad-dir}")
    private String adImageDir;

    private UsersDao testUser;
    private UsersDao adminUser;
    private AdsDao testAd;

    private final String userEmail = "user@test.com";
    private final String userPassword = "password";
    private final String adminEmail = "admin@test.com";
    private final String adminPassword = "admin";

    @BeforeEach
    void setUp() {
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
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        adRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getAllAds_ShouldReturnList() {
        ResponseEntity<AdsDto> response = withAuth(userEmail, userPassword)
                .getForEntity(baseUrl() + "/ads", AdsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCount()).isPositive();
    }

    @Test
    void addAd_ShouldCreateAdAndSaveImage() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        CreateOrUpdateAdDto properties = new CreateOrUpdateAdDto();
        properties.setTitle("New Ad");
        properties.setDescription("New Description");
        properties.setPrice(999);

        String propertiesJson = objectMapper.writeValueAsString(properties);
        ByteArrayResource propertiesPart = new ByteArrayResource(propertiesJson.getBytes()) {
            @Override
            public String getFilename() {
                return "properties.json";
            }
        };

        byte[] imageContent = "fake image content".getBytes();
        ByteArrayResource imagePart = new ByteArrayResource(imageContent) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("properties", propertiesPart);
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<AdDto> response = withAuth(userEmail, userPassword)
                .postForEntity(baseUrl() + "/ads", requestEntity, AdDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("New Ad");
        assertThat(response.getBody().getAuthor()).isEqualTo(testUser.getId());

        String imageUrl = response.getBody().getImage();
        String filename = Paths.get(imageUrl).getFileName().toString();
        Path savedPath = Paths.get(adImageDir).resolve(filename);
        assertThat(savedPath).exists();
        assertThat(Files.readAllBytes(savedPath)).isEqualTo(imageContent);
    }

    @Test
    void getAd_ShouldReturnExtendedAd() {
        ResponseEntity<ExtendedAdDto> response = withAuth(userEmail, userPassword)
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
    void removeAd_ByAuthor_ShouldDeleteAdAndImageFile() throws Exception {
        // Создаём объявление с реальным файлом
        AdsDao adWithImage = new AdsDao();
        adWithImage.setTitle("Ad with image");
        adWithImage.setDescription("Desc");
        adWithImage.setPrice(500);
        adWithImage.setAuthor(testUser);
        adWithImage.setImage("/ads-images/to-delete.jpg");
        adRepository.save(adWithImage);

        Path imagePath = Paths.get(adImageDir, "to-delete.jpg");
        Files.write(imagePath, "content".getBytes());

        ResponseEntity<Void> response = withAuth(userEmail, userPassword)
                .exchange(baseUrl() + "/ads/" + adWithImage.getPk(), HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adRepository.findById(adWithImage.getPk())).isEmpty();
        assertThat(imagePath).doesNotExist();
    }

    @Test
    void removeAd_ByAdmin_ShouldReturnNoContent() {
        ResponseEntity<Void> response = withAuth(adminEmail, adminPassword)
                .exchange(baseUrl() + "/ads/" + testAd.getPk(), HttpMethod.DELETE, null, Void.class);

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

        ResponseEntity<Void> response = withAuth("other@test.com", "password")
                .exchange(baseUrl() + "/ads/" + testAd.getPk(), HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(adRepository.findById(testAd.getPk())).isPresent();
    }

    @Test
    void updateAd_ByAuthor_ShouldReturnUpdatedAd() {
        CreateOrUpdateAdDto update = new CreateOrUpdateAdDto();
        update.setTitle("Updated Title");
        update.setDescription("Updated Description");
        update.setPrice(2000);

        ResponseEntity<AdDto> response = patchWithAuth(
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
        ResponseEntity<AdsDto> response = withAuth(userEmail, userPassword)
                .getForEntity(baseUrl() + "/ads/me", AdsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCount()).isEqualTo(1);
        assertThat(response.getBody().getResults()).hasSize(1);
        assertThat(response.getBody().getResults().get(0).getPk()).isEqualTo(testAd.getPk());
    }

    @Test
    void updateImage_ShouldReplaceImageAndDeleteOld() throws Exception {
        // Создаём старый файл
        Path oldImagePath = Paths.get(adImageDir, "old.jpg");
        Files.write(oldImagePath, "old content".getBytes());
        testAd.setImage("/ads-images/old.jpg");
        adRepository.save(testAd);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        byte[] newContent = "new image content".getBytes();
        ByteArrayResource imagePart = new ByteArrayResource(newContent) {
            @Override
            public String getFilename() {
                return "newimage.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        ResponseEntity<byte[]> response = patchMultipartWithAuth(
                baseUrl() + "/ads/" + testAd.getPk() + "/image",
                body,
                byte[].class,
                userEmail,
                userPassword);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(newContent);

        AdsDao updated = adRepository.findById(testAd.getPk()).orElseThrow();
        assertThat(updated.getImage()).startsWith("/ads-images/");
        assertThat(updated.getImage()).isNotEqualTo(testAd.getImage());

        // Старый файл должен быть удалён
        assertThat(oldImagePath).doesNotExist();

        // Новый файл должен существовать
        String newFilename = Paths.get(updated.getImage()).getFileName().toString();
        Path newImagePath = Paths.get(adImageDir, newFilename);
        assertThat(newImagePath).exists();
        assertThat(Files.readAllBytes(newImagePath)).isEqualTo(newContent);
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
        ByteArrayResource propertiesPart = new ByteArrayResource(propertiesJson.getBytes()) {
            @Override
            public String getFilename() {
                return "properties.json";
            }
        };
        ByteArrayResource imagePart = new ByteArrayResource("image content".getBytes()) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("properties", propertiesPart);
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<AdDto> response = restTemplate.postForEntity(baseUrl() + "/ads", requestEntity, AdDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateAd_WithoutAuth_ShouldReturnUnauthorized() {
        CreateOrUpdateAdDto update = new CreateOrUpdateAdDto();
        update.setTitle("Updated Title");
        update.setDescription("Updated Description");
        update.setPrice(2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("wrong", "creds");
        HttpEntity<CreateOrUpdateAdDto> requestEntity = new HttpEntity<>(update, headers);

        ResponseEntity<AdDto> response = restTemplate.exchange(
                baseUrl() + "/ads/" + testAd.getPk(),
                HttpMethod.PATCH,
                requestEntity,
                AdDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void removeAd_WithoutAuth_ShouldReturnUnauthorized() {
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/ads/" + testAd.getPk(),
                HttpMethod.DELETE,
                null,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getAdsMe_WithoutAuth_ShouldReturnUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/ads/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateImage_WithoutAuth_ShouldReturnUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource imagePart = new ByteArrayResource("new image".getBytes()) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/ads/" + testAd.getPk() + "/image",
                HttpMethod.PATCH,
                requestEntity,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ====== ТЕСТЫ на 404 для несуществующего объявления ======

    @Test
    void getAd_NotFound_ShouldReturn404() {
        ResponseEntity<String> response = withAuth(userEmail, userPassword)
                .getForEntity(baseUrl() + "/ads/999999", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAd_NotFound_ShouldReturn404() {
        CreateOrUpdateAdDto update = new CreateOrUpdateAdDto();
        update.setTitle("Updated Title");
        update.setDescription("Updated Description");
        update.setPrice(2000);

        ResponseEntity<String> response = patchWithAuth(
                baseUrl() + "/ads/999999",
                update,
                String.class,
                userEmail,
                userPassword);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removeAd_NotFound_ShouldReturn404() {
        ResponseEntity<Void> response = withAuth(userEmail, userPassword)
                .exchange(baseUrl() + "/ads/999999", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateImage_NotFound_ShouldReturn404() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBasicAuth(userEmail, userPassword);

        ByteArrayResource imagePart = new ByteArrayResource("new image".getBytes()) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/ads/999999/image",
                HttpMethod.PATCH,
                requestEntity,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ====== ТЕСТЫ на 403 (обновление чужого объявления) ======

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

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/ads/" + testAd.getPk(),
                HttpMethod.PATCH,
                requestEntity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}