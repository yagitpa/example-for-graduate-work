package ru.skypro.homework.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.user.NewPasswordDto;
import ru.skypro.homework.dto.user.UpdateUserDto;
import ru.skypro.homework.dto.user.UserDto;
import ru.skypro.homework.exception.InvalidCurrentPasswordException;
import ru.skypro.homework.exception.UserNotFoundException;
import ru.skypro.homework.mapper.UserMapper;
import ru.skypro.homework.model.UsersDao;
import ru.skypro.homework.repository.UserRepository;
import ru.skypro.homework.service.UserService;
import ru.skypro.homework.util.ImageHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.image.avatar-dir}")
    private String avatarDir;

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(String email) {
        UsersDao user = getUserByEmail(email);
        return userMapper.toUserDto(user);
    }

    @Override
    public UpdateUserDto updateUser(String email, UpdateUserDto updateUserDto) {
        UsersDao user = getUserByEmail(email);
        userMapper.updateUserFromDto(updateUserDto, user);
        userRepository.save(user);
        return updateUserDto;
    }

    @Override
    public void setPassword(String email, NewPasswordDto newPasswordDto) {
        UsersDao user = getUserByEmail(email);
        if (!passwordEncoder.matches(newPasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPasswordDto.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", email);
    }

    @Override
    public void updateUserImage(String email, MultipartFile image) {
        UsersDao user = getUserByEmail(email);
        try {
            String extension = ImageHelper.getExtension(image.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path uploadPath = Paths.get(avatarDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(filename);
            image.transferTo(filePath.toFile());

            // Удаление старого аватара, если есть
            if (user.getImage() != null) {
                Path oldFilePath = Paths.get(user.getImage());
                Files.deleteIfExists(oldFilePath);
            }

            // Сохраняем относительный путь
            String relativePath = "/avatars/" + filename;
            user.setImage(relativePath);
            userRepository.save(user);
            log.info("Avatar updated for user: {}", email);
        } catch (IOException e) {
            log.error("Failed to save avatar for user: {}", email, e);
            throw new RuntimeException("Failed to save avatar", e);
        }
    }

    private UsersDao getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                             .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
}