package ru.skypro.homework.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import ru.skypro.homework.exception.ImageNotFoundException;
import ru.skypro.homework.exception.InvalidImageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageServiceTest {

    private ImageService imageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        imageService = new ImageService();
        // Подменяем директории для тестов через пакетные сеттеры
        imageService.setAvatarDir(tempDir.toString());
        imageService.setAdImageDir(tempDir.toString());
    }

    @Test
    void saveImage_shouldSaveFileAndReturnUrl() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());
        String directory = tempDir.toString();
        String urlPrefix = "/test-prefix/";

        // when
        String savedUrl = imageService.saveImage(file, directory, urlPrefix);

        // then
        assertThat(savedUrl).startsWith(urlPrefix);
        String filename = savedUrl.substring(urlPrefix.length());
        Path savedPath = tempDir.resolve(filename);
        assertThat(savedPath).exists();
    }

    @Test
    void saveImage_shouldThrowInvalidImageException_whenContentTypeNotAllowed() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.txt", "text/plain", "test content".getBytes());

        // when/then
        assertThatThrownBy(() -> imageService.saveImage(file, tempDir.toString(), "/prefix"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Неподдерживаемый тип файла");
    }

    @Test
    void saveImage_shouldThrowInvalidImageException_whenFileIsEmpty() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image", "empty.jpg", "image/jpeg", new byte[0]);

        // when/then
        assertThatThrownBy(() -> imageService.saveImage(emptyFile, tempDir.toString(), "/prefix"))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("файл пуст");
    }

    @Test
    void deleteImage_shouldDeleteFile_whenExists() throws IOException {
        // given
        Path testFile = tempDir.resolve("test.jpg");
        Files.write(testFile, "content".getBytes());
        String imagePath = "/some/prefix/test.jpg";

        // when
        imageService.deleteImage(imagePath, tempDir.toString());

        // then
        assertThat(testFile).doesNotExist();
    }

    @Test
    void deleteImage_shouldNotThrow_whenFileNotExists() {
        // when/then
        imageService.deleteImage("/non/existing.jpg", tempDir.toString());
        // просто не должно быть исключения
    }

    @Test
    void loadAdImage_shouldReturnImageData_whenFileExists() throws IOException {
        // given
        String filename = "ad.jpg";
        Path filePath = tempDir.resolve(filename);
        byte[] content = "fake ad image".getBytes();
        Files.write(filePath, content);

        // when
        ImageService.ImageData imageData = imageService.loadAdImage(filename);

        // then
        assertThat(imageData.getContent()).isEqualTo(content);
        assertThat(imageData.getContentType()).isEqualTo("image/jpeg");
    }

    @Test
    void loadAdImage_shouldThrowImageNotFoundException_whenFileNotExists() {
        // when/then
        assertThatThrownBy(() -> imageService.loadAdImage("missing.jpg"))
                .isInstanceOf(ImageNotFoundException.class)
                .hasMessageContaining("не существует");
    }
}