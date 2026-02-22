package ru.skypro.homework.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import ru.skypro.homework.constants.ExceptionMessages;
import ru.skypro.homework.exception.UserNotFoundException;
import ru.skypro.homework.model.UsersDao;
import ru.skypro.homework.repository.UserRepository;

/**
 * Сервис для получения информации о текущем аутентифицированном пользователе. Содержит методы для
 * получения email и полной сущности {@link UsersDao} из контекста безопасности.
 *
 * <p>При отсутствии аутентификации выбрасывает {@link AuthenticationException}.
 *
 * @see UserRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Возвращает email текущего аутентифицированного пользователя.
     *
     * @throws AuthenticationException если пользователь не аутентифицирован (включая анонимного)
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationException(ExceptionMessages.USER_NOT_AUTHENTICATED) {};
        }
        return authentication.getName();
    }

    /**
     * Возвращает сущность текущего аутентифицированного пользователя.
     *
     * @throws AuthenticationException если пользователь не аутентифицирован
     * @throws UserNotFoundException если пользователь не найден в БД
     */
    public UsersDao getCurrentUser() {
        String email = getCurrentUserEmail();
        log.debug("Fetching current user with email: {}", email);
        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () ->
                                new UserNotFoundException(
                                        String.format(ExceptionMessages.USER_NOT_FOUND, email)));
    }

    public UsersDao getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () ->
                                new UserNotFoundException(
                                        String.format(ExceptionMessages.USER_NOT_FOUND, email)));
    }
}
