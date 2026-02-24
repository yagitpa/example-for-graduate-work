package ru.skypro.homework.controller.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.skypro.homework.AbstractIntegrationTest;
import ru.skypro.homework.dto.auth.Role;
import ru.skypro.homework.dto.user.NewPasswordDto;
import ru.skypro.homework.dto.user.UpdateUserDto;
import ru.skypro.homework.dto.user.UserDto;
import ru.skypro.homework.model.UsersDao;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private AdRepository adRepository;
    @Autowired private CommentRepository commentRepository;

    @Value("${app.image.avatar-dir}")
    private String avatarDir;

    private UsersDao testUser;
    private final String userEmail = "user@test.com";
    private final String userPassword = "password";

    @BeforeEach
    void setUp() {
        testUser = new UsersDao();
        testUser.setEmail(userEmail);
        testUser.setPassword(passwordEncoder.encode(userPassword));
        testUser.setFirstName("Иван");
        testUser.setLastName("Иванов");
        testUser.setPhone("+7 (999) 123-45-67");
        testUser.setRole(Role.USER);
        testUser.setImage("/avatars/old.jpg");
        userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        adRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getUser_ShouldReturnUserInfo() {
        ResponseEntity<UserDto> response = withAuth(userEmail, userPassword)
                .getForEntity(baseUrl() + "/users/me", UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(testUser.getId());
        assertThat(response.getBody().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getBody().getFirstName()).isEqualTo(testUser.getFirstName());
        assertThat(response.getBody().getLastName()).isEqualTo(testUser.getLastName());
        assertThat(response.getBody().getPhone()).isEqualTo(testUser.getPhone());
        assertThat(response.getBody().getRole()).isEqualTo(testUser.getRole());
        assertThat(response.getBody().getImage()).isEqualTo(testUser.getImage());
    }

    @Test
    void updateUser_ShouldReturnUpdatedInfo() {
        UpdateUserDto update = new UpdateUserDto();
        update.setFirstName("Пётр");
        update.setLastName("Петров");
        update.setPhone("+7 (999) 999-99-99");

        ResponseEntity<UpdateUserDto> response = patchWithAuth(
                baseUrl() + "/users/me",
                update,
                UpdateUserDto.class,
                userEmail,
                userPassword);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFirstName()).isEqualTo("Пётр");
        assertThat(response.getBody().getLastName()).isEqualTo("Петров");
        assertThat(response.getBody().getPhone()).isEqualTo("+7 (999) 999-99-99");

        UsersDao updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Пётр");
        assertThat(updated.getLastName()).isEqualTo("Петров");
        assertThat(updated.getPhone()).isEqualTo("+7 (999) 999-99-99");
    }

    @Test
    void setPassword_ShouldChangePassword() {
        NewPasswordDto passwordDto = new NewPasswordDto();
        passwordDto.setCurrentPassword(userPassword);
        passwordDto.setNewPassword("newPassword");

        HttpEntity<NewPasswordDto> request = new HttpEntity<>(passwordDto);
        ResponseEntity<Void> response = withAuth(userEmail, userPassword)
                .postForEntity(baseUrl() + "/users/set_password", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<UserDto> failedLogin = withAuth(userEmail, userPassword)
                .getForEntity(baseUrl() + "/users/me", UserDto.class);
        assertThat(failedLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<UserDto> successLogin = withAuth(userEmail, "newPassword")
                .getForEntity(baseUrl() + "/users/me", UserDto.class);
        assertThat(successLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void setPassword_WithWrongCurrent_ShouldReturnBadRequest() {
        NewPasswordDto passwordDto = new NewPasswordDto();
        passwordDto.setCurrentPassword("wrong123");
        passwordDto.setNewPassword("newPassword");

        HttpEntity<NewPasswordDto> request = new HttpEntity<>(passwordDto);
        ResponseEntity<Void> response = withAuth(userEmail, userPassword)
                .postForEntity(baseUrl() + "/users/set_password", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateUserImage_ShouldReturnOk() throws Exception {
        // given
        byte[] imageContent = "new avatar".getBytes();
        ByteArrayResource imagePart = new ByteArrayResource(imageContent) {
            @Override
            public String getFilename() {
                return "avatar.jpg";
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        // when
        ResponseEntity<Void> response = patchMultipartWithAuth(
                baseUrl() + "/users/me/image",
                body,
                Void.class,
                userEmail,
                userPassword);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Проверяем, что в БД обновился путь
        UsersDao updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getImage()).startsWith("/avatars/");
        assertThat(updated.getImage()).isNotEqualTo(testUser.getImage());

        // Извлекаем имя файла из пути и проверяем его существование в реальной директории
        String imagePath = updated.getImage();
        String filename = Paths.get(imagePath).getFileName().toString();
        Path expectedPath = Paths.get(avatarDir).resolve(filename);

        assertThat(expectedPath).exists();
        assertThat(Files.readAllBytes(expectedPath)).isEqualTo(imageContent);
    }

    @Test
    void getUser_WithoutAuth_ShouldReturnUnauthorized() {
        ResponseEntity<UserDto> response = restTemplate.getForEntity(baseUrl() + "/users/me", UserDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateUser_WithoutAuth_ShouldReturnUnauthorized() {
        UpdateUserDto update = new UpdateUserDto();
        update.setFirstName("Пётр");
        update.setLastName("Петров");
        update.setPhone("+7 (999) 999-99-99");

        HttpEntity<UpdateUserDto> requestEntity = new HttpEntity<>(update);
        ResponseEntity<UpdateUserDto> response = restTemplate.exchange(
                baseUrl() + "/users/me",
                HttpMethod.PATCH,
                requestEntity,
                UpdateUserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void setPassword_WithoutAuth_ShouldReturnUnauthorized() {
        NewPasswordDto passwordDto = new NewPasswordDto();
        passwordDto.setCurrentPassword("any");
        passwordDto.setNewPassword("new");

        HttpEntity<NewPasswordDto> requestEntity = new HttpEntity<>(passwordDto);
        ResponseEntity<Void> response = restTemplate.postForEntity(
                baseUrl() + "/users/set_password", requestEntity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateUserImage_WithoutAuth_ShouldReturnUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource imagePart = new ByteArrayResource("new avatar".getBytes()) {
            @Override
            public String getFilename() {
                return "avatar.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/users/me/image",
                HttpMethod.PATCH,
                requestEntity,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}