package ru.skypro.homework.controller.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.skypro.homework.AbstractIntegrationTest;
import ru.skypro.homework.service.ImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ImageService imageService;

    @Value("${app.image.avatar-dir}")
    private String avatarDir;

    @Value("${app.image.ad-dir}")
    private String adImageDir;

    @Test
    void getAvatar_ShouldReturnImage_WhenExists() throws IOException {
        // given
        Path avatarPath = Paths.get(avatarDir, "avatar.jpg");
        byte[] content = "fake-avatar-content".getBytes();
        Files.write(avatarPath, content);
        assertThat(Files.exists(avatarPath)).isTrue();

        // дополнительная проверка: загружается ли файл через сервис напрямую
        ImageService.ImageData serviceData = imageService.loadAvatar("avatar.jpg");
        assertThat(serviceData.getContent()).isEqualTo(content);

        // when
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                baseUrl() + "/avatars/avatar.jpg", byte[].class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(content);
    }

    @Test
    void getAvatar_ShouldReturn404_WhenNotExists() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                baseUrl() + "/avatars/nonexistent.jpg", byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAdImage_ShouldReturnImage_WhenExists() throws IOException {
        // given
        Path adPath = Paths.get(adImageDir, "ad.jpg");
        byte[] content = "fake-ad-content".getBytes();
        Files.write(adPath, content);
        assertThat(Files.exists(adPath)).isTrue();

        // дополнительная проверка через сервис
        ImageService.ImageData serviceData = imageService.loadAdImage("ad.jpg");
        assertThat(serviceData.getContent()).isEqualTo(content);

        // when
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                baseUrl() + "/ads-images/ad.jpg", byte[].class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(content);
    }

    @Test
    void getAdImage_ShouldReturn404_WhenNotExists() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                baseUrl() + "/ads-images/nonexistent.jpg", byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}