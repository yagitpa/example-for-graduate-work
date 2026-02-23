package ru.skypro.homework.service;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ru.skypro.homework.constants.ExceptionMessages;
import ru.skypro.homework.constants.ValidationConstants;
import ru.skypro.homework.exception.ImageNotFoundException;
import ru.skypro.homework.exception.ImageReadException;
import ru.skypro.homework.exception.ImageSaveException;
import ru.skypro.homework.exception.InvalidImageException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import javax.annotation.PostConstruct;

/**
 * Сервис для работы с изображениями. Обеспечивает сохранение, удаление, чтение файлов изображений,
 * а также загрузку их с определением MIME-типа.
 *
 * <p>При ошибках ввода-вывода выбрасывает {@link ImageReadException}, при отсутствии файла – {@link
 * ImageNotFoundException}.
 */
@Getter
@Setter
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${app.image.avatar-dir}")
    private String avatarDir;

    @Value("${app.image.ad-dir}")
    private String adImageDir;

    public ImageData loadAdImage(String filename) {
        return loadImage(filename, adImageDir);
    }

    public ImageData loadAvatar(String filename) {
        return loadImage(filename, avatarDir);
    }

    @PostConstruct
    public void init() {
        createDirectoryIfNotExists(avatarDir);
        createDirectoryIfNotExists(adImageDir);
    }

    /**
     * Сохраняет файл в указанную директорию.
     *
     * @param image загружаемый файл
     * @param directory корневая директория для сохранения (например, "./avatars")
     * @param urlPrefix префикс URL для доступа (например, "/avatars/")
     * @return относительный путь к файлу (например, "/avatars/file.jpg")
     */
    public String saveImage(MultipartFile image, String directory, String urlPrefix) {
        validateImage(image);
        try {
            String extension = getExtension(image.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path uploadPath = Paths.get(directory);
            // Логируем полный путь и имя файла
            log.debug("Сохранение изображения в директорию: {}, имя файла: {}", uploadPath.toAbsolutePath(), filename);
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            image.transferTo(filePath.toFile());
            log.info("Изображение успешно сохранено: {}", filePath.toAbsolutePath());
            return urlPrefix + filename;
        } catch (IOException e) {
            log.error("Ошибка сохранения изображения в {}", directory, e);
            throw new ImageSaveException(ExceptionMessages.IMAGE_FAILED_TO_SAVE, e);
        }
    }

    public String saveAvatar(MultipartFile image, Integer userId, String urlPrefix) {
        validateImage(image);
        try {
            String extension = getExtension(image.getOriginalFilename());
            String filename = userId + extension;
            Path uploadPath = Paths.get(avatarDir);
            log.debug("Сохранение аватара для userId={} в директорию: {}, имя файла: {}", userId, uploadPath.toAbsolutePath(), filename);

            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);

            // Сохраняем через Files.write (просто и надёжно)
            byte[] bytes = image.getBytes();
            Files.write(filePath, bytes);

            // Проверяем, что файл действительно создан
            if (Files.exists(filePath)) {
                long size = Files.size(filePath);
                log.info("Файл {} успешно создан, размер: {} байт", filePath.toAbsolutePath(), size);
            } else {
                log.error("Файл {} НЕ СУЩЕСТВУЕТ после записи!", filePath.toAbsolutePath());
            }

            return urlPrefix + filename;
        } catch (IOException e) {
            log.error("Ошибка сохранения аватара для пользователя {}", userId, e);
            throw new ImageSaveException(ExceptionMessages.IMAGE_FAILED_TO_SAVE, e);
        }
    }

    /**
     * Удаляет файл по его относительному пути.
     *
     * @param imagePath относительный путь (например, "/avatars/file.jpg")
     * @param directory корневая директория (например, "./avatars")
     */
    public void deleteImage(String imagePath, String directory) {
        if (imagePath == null) return;
        try {
            Path fullPath = Paths.get(directory, Paths.get(imagePath).getFileName().toString());
            Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            log.warn("Failed to delete image file: {}", imagePath, e);
        }
    }

    /**
     * Читает файл в массив байт.
     *
     * @param imagePath относительный путь к файлу
     * @param directory корневая директория
     * @return массив байт файла
     */
    public byte[] readImageAsBytes(String imagePath, String directory) {
        try {
            Path fullPath = Paths.get(directory, Paths.get(imagePath).getFileName().toString());
            return Files.readAllBytes(fullPath);
        } catch (IOException e) {
            log.error("Failed to read image file: {}", imagePath, e);
            throw new RuntimeException(
                    String.format(ExceptionMessages.IMAGE_FAILED_TO_READ, imagePath), e);
        }
    }

    /**
     * Загружает изображение из указанной директории.
     *
     * @param filename имя файла
     * @param directory директория (avatarDir или adImageDir)
     * @return объект {@link ImageData} с содержимым и MIME-типом, или null если файл не найден
     * @throws ImageNotFoundException исключение при ошибке "изображение не найдено"
     * @throws ImageReadException исключение при ошибке чтения изображения
     */
    private ImageData loadImage(String filename, String directory) {
        try {
            Path path = Paths.get(directory, filename);
            if (!Files.exists(path)) {
                throw new ImageNotFoundException(
                        String.format(ExceptionMessages.IMAGE_NOT_FOUND, filename));
            }
            byte[] content = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new ImageData(content, contentType);
        } catch (IOException e) {
            throw new ImageReadException(
                    String.format(ExceptionMessages.IMAGE_FAILED_TO_READ, filename), e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private void createDirectoryIfNotExists(String dir) {
        Path path = Paths.get(dir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("Created directory: {}", path.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to create directory: {}", path.toAbsolutePath(), e);
            }
        }
    }

    /**
     * Валидация входящих изображений: пустой файл, неподдерживаемый тип файла и превышение разрешенного размера файла
     *
     * @param image
     */
    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidImageException(ExceptionMessages.INVALID_IMAGE_FILE);
        }
        String contentType = image.getContentType();
        if (contentType == null || !ValidationConstants.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidImageException(String.format(ExceptionMessages.INVALID_IMAGE_TYPE, contentType));
        }
        if (image.getSize() > ValidationConstants.MAX_FILE_SIZE) {
            throw new InvalidImageException(ExceptionMessages.INVALID_IMAGE_SIZE);
        }
    }

    /** Вспомогательный класс для передачи данных изображения. */
    @Data
    public static class ImageData {
        private final byte[] content;
        private final String contentType;
    }
}
