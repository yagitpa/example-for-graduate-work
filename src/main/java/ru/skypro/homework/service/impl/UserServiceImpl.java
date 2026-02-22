package ru.skypro.homework.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.constants.ExceptionMessages;
import ru.skypro.homework.constants.UrlPrefixConstants;
import ru.skypro.homework.dto.user.NewPasswordDto;
import ru.skypro.homework.dto.user.UpdateUserDto;
import ru.skypro.homework.dto.user.UserDto;
import ru.skypro.homework.exception.InvalidCurrentPasswordException;
import ru.skypro.homework.mapper.UserMapper;
import ru.skypro.homework.model.UsersDao;
import ru.skypro.homework.repository.UserRepository;
import ru.skypro.homework.service.CurrentUserService;
import ru.skypro.homework.service.ImageService;
import ru.skypro.homework.service.UserService;

/**
 * Реализация сервиса {@link UserService}.
 * Использует {@link UserRepository} для доступа к данным пользователей,
 * {@link PasswordEncoder} для шифрования паролей,
 * {@link CurrentUserService} для получения текущего аутентифицированного пользователя,
 * {@link ImageService} для сохранения и удаления аватаров.
 *
 * @see UserService
 * @see UserRepository
 * @see UserMapper
 * @see CurrentUserService
 * @see ImageService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final ImageService imageService;

    @Value("${app.image.avatar-dir}")
    private String avatarDir;

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser() {
        UsersDao user = currentUserService.getCurrentUser();
        return userMapper.toUserDto(user);
    }

    @Override
    public UpdateUserDto updateUser(UpdateUserDto updateUserDto) {
        UsersDao user = currentUserService.getCurrentUser();
        userMapper.updateUserFromDto(updateUserDto, user);
        userRepository.save(user);
        return updateUserDto;
    }

    @Override
    public void setPassword(NewPasswordDto newPasswordDto) {
        UsersDao user = currentUserService.getCurrentUser();
        if (!passwordEncoder.matches(newPasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException(ExceptionMessages.INVALID_CURRENT_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(newPasswordDto.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Override
    public void updateUserImage(MultipartFile image) {
        UsersDao user = currentUserService.getCurrentUser();
        String newImagePath = imageService.saveImage(image, avatarDir, UrlPrefixConstants.URL_PREFIX_AVATARS);
        if (user.getImage() != null) {
            imageService.deleteImage(user.getImage(), avatarDir);
        }
        user.setImage(newImagePath);
        userRepository.save(user);
    }
}