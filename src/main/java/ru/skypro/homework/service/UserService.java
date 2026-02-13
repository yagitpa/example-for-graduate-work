package ru.skypro.homework.service;

import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.dto.user.NewPasswordDto;
import ru.skypro.homework.dto.user.UpdateUserDto;
import ru.skypro.homework.dto.user.UserDto;

public interface UserService {

    /**
     * Получение информации о текущем пользователе.
     *
     * @param email email (username) текущего пользователя
     * @return UserDto с данными пользователя
     */
    UserDto getUser(String email);

    /**
     * Обновление данных текущего пользователя (имя, фамилия, телефон).
     *
     * @param email         email текущего пользователя
     * @param updateUserDto новые данные
     * @return обновлённые данные
     */
    UpdateUserDto updateUser(String email, UpdateUserDto updateUserDto);

    /**
     * Смена пароля текущего пользователя.
     *
     * @param email          email текущего пользователя
     * @param newPasswordDto данные старого и нового пароля
     */
    void setPassword(String email, NewPasswordDto newPasswordDto);

    /**
     * Обновление аватара текущего пользователя.
     *
     * @param email email текущего пользователя
     * @param image файл нового аватара
     */
    void updateUserImage(String email, MultipartFile image);
}