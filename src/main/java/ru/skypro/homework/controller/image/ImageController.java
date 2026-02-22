package ru.skypro.homework.controller.image;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.skypro.homework.service.ImageService;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/ads-images/{filename}")
    public ResponseEntity<byte[]> getAdImage(@PathVariable String filename) {
        ImageService.ImageData imageData = imageService.loadAdImage(filename);
        return ResponseEntity.ok()
                             .contentType(MediaType.parseMediaType(imageData.getContentType()))
                             .body(imageData.getContent());
    }

    @GetMapping("/avatars/{filename}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String filename) {
        ImageService.ImageData imageData = imageService.loadAvatar(filename);
        return ResponseEntity.ok()
                             .contentType(MediaType.parseMediaType(imageData.getContentType()))
                             .body(imageData.getContent());
    }
}