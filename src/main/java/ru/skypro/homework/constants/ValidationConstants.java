package ru.skypro.homework.constants;

import java.util.List;

public final class ValidationConstants {

    private ValidationConstants() {}

    // ------ Пользователь (авторизация и регистрация) ------
    public static final int USERNAME_MIN_SIZE = 4;
    public static final int USERNAME_MAX_SIZE = 32;

    public static final int PASSWORD_MIN_SIZE = 8;
    public static final int PASSWORD_MAX_SIZE = 16;

    public static final int FIRST_NAME_MIN_SIZE = 2;
    public static final int FIRST_NAME_MAX_SIZE = 16;
    public static final int LAST_NAME_MIN_SIZE = 2;
    public static final int LAST_NAME_MAX_SIZE = 16;

    // ----- Телефон -----
    public static final String PHONE_PATTERN = "\\+7\\s?\\(?\\d{3}\\)?\\s?\\d{3}-?\\d{2}-?\\d{2}";
    public static final String PHONE_EXAMPLE = "+7 (999) 999-99-99";

    // ----- Объявления -----
    public static final int AD_TITLE_MIN_SIZE = 4;
    public static final int AD_TITLE_MAX_SIZE = 32;

    public static final int AD_DESCRIPTION_MIN_SIZE = 8;
    public static final int AD_DESCRIPTION_MAX_SIZE = 64;

    public static final int AD_PRICE_MIN = 0;
    public static final int AD_PRICE_MAX = 10_000_000;

    // ----- Комментарии -----
    public static final int COMMENT_TEXT_MIN_SIZE = 8;
    public static final int COMMENT_TEXT_MAX_SIZE = 64;

    // ----- Изображения -----
    public static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp");
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
}
