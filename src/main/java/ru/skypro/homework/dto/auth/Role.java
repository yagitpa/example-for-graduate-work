package ru.skypro.homework.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "UserRole",
        description = "Роли пользователей в системе",
        type = "string",
        enumAsRef = true,
        allowableValues = {"USER", "ADMIN"})
public enum Role {
    USER,
    ADMIN
}
