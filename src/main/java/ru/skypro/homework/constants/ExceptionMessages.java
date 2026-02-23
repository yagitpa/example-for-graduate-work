package ru.skypro.homework.constants;

public final class ExceptionMessages {

    // ----- Объявления -----
    public static final String AD_NOT_FOUND = "Объявление с id %d не найдено";

    // ----- Комментарии -----
    public static final String COMMENT_NOT_FOUND =
            "Комментарий с id %d для объявления %d не найден";

    // ----- Пользователи -----
    public static final String USER_NOT_FOUND = "Пользователя с email %s не существует";
    public static final String USER_NOT_FOUND_BY_ID = "Пользователя с id %d не существует";
    public static final String USER_ALREADY_EXISTS =
            "Пользователь с указанным email %s уже существует";
    public static final String USER_NOT_AUTHENTICATED = "Пользователь не авторизован";
    public static final String INVALID_CURRENT_PASSWORD = "Указан неверный текущий пароль";
    public static final String UNAUTHORIZED_ACCESS = "У пользователя нет прав на изменение %s";

    // ----- Изображения -----
    public static final String IMAGE_NOT_FOUND = "Изображения %s не существует";
    public static final String IMAGE_FAILED_TO_READ = "Ошибка чтения изображения %s";
    public static final String IMAGE_FAILED_TO_SAVE = "Ошибка сохранения изображения";
    public static final String INVALID_IMAGE_FILE = "Загруженный файл пуст или отсутствует";
    public static final String INVALID_IMAGE_TYPE = "Неподдерживаемый тип файла: %s";
    public static final String INVALID_IMAGE_SIZE = "Размер файла превышает допустимый (максимум 5 Мб)";

    // ----- Прочее -----
    public static final String USE_ACTUAL_REGISTER_METHOD =
            "Для регистрации должен использоваться корректный метод с полным набором данных";
    public static final String USE_ACTUAL_UPDATE_METHOD =
            "Для обновления информации должен использоваться корректный метод с полным набором данных";

    private ExceptionMessages() {}
}
