package ru.skypro.homework.service;

import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.user.NewPasswordDto;
import ru.skypro.homework.dto.user.UpdateUserDto;
import ru.skypro.homework.dto.user.UserDto;

/**
 * Сервис для управления пользователями.
 * Предоставляет методы для получения информации о текущем пользователе,
 * обновления данных, смены пароля и загрузки аватара.
 */
public interface UserService {

    /**
     * Получение информации о текущем пользователе.
     *
     * @return UserDto с данными пользователя
     */
    UserDto getUser();

    /**
     * Обновление данных текущего пользователя (имя, фамилия, телефон).
     *
     * @param updateUserDto новые данные
     * @return обновлённые данные
     */
    UpdateUserDto updateUser(UpdateUserDto updateUserDto);

    /**
     * Смена пароля текущего пользователя.
     *
     * @param newPasswordDto данные старого и нового пароля
     */
    void setPassword(NewPasswordDto newPasswordDto);

    /**
     * Обновление аватара текущего пользователя.
     *
     * @param image файл нового аватара
     */
    void updateUserImage(MultipartFile image);
}