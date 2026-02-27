package ru.skypro.homework.controller.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ru.skypro.homework.service.ImageService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Загрузка изображений")
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "Получение картинки объявления")
    @GetMapping("/ads-images/{filename}")
    public ResponseEntity<byte[]> getAdImage(@PathVariable String filename) {
        ImageService.ImageData imageData = imageService.loadAdImage(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageData.getContentType()))
                .body(imageData.getContent());
    }

    @Operation(summary = "Получение аватара пользователя")
    @GetMapping("/avatars/{filename}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String filename) {
        ImageService.ImageData imageData = imageService.loadAvatar(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageData.getContentType()))
                .body(imageData.getContent());
    }
}
