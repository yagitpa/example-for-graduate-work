package ru.skypro.homework.controller.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.skypro.homework.AbstractIntegrationTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ImageController imageController;

    private static final String AVATAR_DIR = "./target/test-avatars";
    private static final String AD_IMAGE_DIR = "./target/test-ads-images";

    @BeforeEach
    void setUp() throws IOException {
        createImageDirectories();
        // Создание тестовых файлы
        Files.write(Paths.get(AVATAR_DIR, "avatar.jpg"), "fake-avatar-content".getBytes());
        Files.write(Paths.get(AD_IMAGE_DIR, "ad.jpg"), "fake-ad-content".getBytes());
    }

    @Test
    void getAvatar_ShouldReturnImage_WhenExists() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(baseUrl() + "/avatars/avatar.jpg", byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAvatar_ShouldReturn404_WhenNotExists() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(baseUrl() + "/avatars/nonexistent.jpg", byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAdImage_ShouldReturnImage_WhenExists() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(baseUrl() + "/ads-images/ad.jpg", byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAdImage_ShouldReturn404_WhenNotExists() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(baseUrl() + "/ads-images/nonexistent.jpg", byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}