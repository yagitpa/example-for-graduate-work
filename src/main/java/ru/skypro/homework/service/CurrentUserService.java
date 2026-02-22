package ru.skypro.homework.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import ru.skypro.homework.exception.UserNotFoundException;
import ru.skypro.homework.model.UsersDao;
import ru.skypro.homework.repository.UserRepository;

/**
 * Сервис для получения пользователя по email. Используется в других сервисах.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Возвращает email текущего аутентифицированного пользователя.
     * @throws AuthenticationException если пользователь не аутентифицирован (включая анонимного)
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationException("User not authenticated") {};
        }
        return authentication.getName();
    }

    /**
     * Возвращает сущность текущего аутентифицированного пользователя.
     * @throws AuthenticationException если пользователь не аутентифицирован
     * @throws UserNotFoundException если пользователь не найден в БД
     */
    public UsersDao getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                             .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    public UsersDao getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                             .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
}