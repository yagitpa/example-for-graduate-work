# Ads Platform

Проект "Ads Platform" — это backend-часть веб-приложения для размещения объявлений, написанная на Java с использованием Spring Boot. Приложение предоставляет REST API для управления пользователями, объявлениями и комментариями, а также для загрузки и получения изображений (аватаров и картинок объявлений).

[Скринкаст дипломной работы на YouTube](https://www.youtube.com/watch?v=hl3wlw-JaD4)

## Используемые технологии

- **Java 11**
- **Spring Boot 2.7.15** (Web, Data JPA, Security, Validation)
- **Spring Security** (Basic Authentication, CORS)
- **PostgreSQL** (основная БД)
- **Flyway** (миграции БД)
- **MapStruct** (маппинг DTO ↔ Entity)
- **Lombok** (упрощение кода)
- **Testcontainers** (интеграционные тесты с реальной БД)
- **JUnit 5, AssertJ, Mockito** (тестирование)
- **Maven** (сборка)
- **OpenAPI (Swagger)** — документация API доступна по `/swagger-ui.html` (локально)

## Структура проекта

src/main/java/ru/skypro/homework/
- `config` – конфигурационные классы (Security, OpenAPI, MapStruct)
- `constants` – константы для валидации и сообщений об ошибках
- `controller` – REST-контроллеры (`ad`, `auth`, `comment`, `image`, `user`)
- `dto` – Data Transfer Objects (запросы/ответы)
- `exception` – кастомные исключения
- `filter` – фильтры (CORS), в данной реализации не используется, функционал покрыт настройками `WebSecurityConfig`
- `handler` – глобальный обработчик ошибок
- `mapper` – мапперы MapStruct
- `model` – сущности JPA (`AdsDao`, `CommentsDao`, `UsersDao`)
- `repository` – Spring Data JPA репозитории
- `service` – бизнес-логика и реализации

## Требования

- **Java 11** или выше
- **Maven 3.6+**
- **PostgreSQL** (локально или в Docker)
- **Docker v. 4.37.0+, Engine 27.4.0+, Compose 2.31.0+** (для запуска тестов с Testcontainers, опционально)

## Сборка и запуск

1. Клонируйте репозиторий:
    ```bash
    git clone https://github.com/yagitpa/diplom.git
    cd diplom
    ```
2. Создайте базу данных (например, ads_db) в PostgreSQL.

3. Настройте параметры подключения в application.properties (или через переменные окружения):
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/ads_db
    spring.datasource.username=postgres
    spring.datasource.password=12345
    ```
4. Соберите проект:
    ```bash
    mvn clean package
    ```
5. Запустите приложение:
    ```bash
    java -jar target/ads-0.0.1-SNAPSHOT.jar
    ```
Приложение будет доступно по адресу `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Тестирование
Проект содержит юнит-тесты и интеграционные тесты. Для запуска всех тестов выполните:
    ```bash
    mvn clean test
    ```
Интеграционные тесты используют Testcontainers и автоматически поднимают контейнер с PostgreSQL. Убедитесь, что Docker установлен и запущен.

## API
Полная спецификация API доступна в формате OpenAPI на странице Swagger.
Основные эндпоинты:
- Регистрация / авторизация: `POST /register`, `POST /login`
- Пользователи: `GET /users/me`, `PATCH /users/me`, `PATCH /users/me/image`, `POST /users/set_password`
- Объявления: `GET /ads`, `POST /ads`, `GET /ads/{id}`, `DELETE /ads/{id}`, `PATCH /ads/{id}`, `GET /ads/me`, `PATCH /ads/{id}/image`
- Комментарии: `GET /ads/{adId}/comments`, `POST /ads/{adId}/comments`, `DELETE /ads/{adId}/comments/{commentId}`, `PATCH /ads/{adId}/comments/{commentId}`
- Изображения: `GET /ads-images/{filename}`, `GET /avatars/{filename}`

## UML-диаграмма

![UML](https://s10.iimage.su/s/27/gSiQBJWxwEhciAl6nnOEBA7IUYZHPSF1n9qy6pl4N.png)
