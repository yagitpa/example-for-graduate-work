package ru.skypro.homework.controller.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.AbstractIntegrationTest;
import ru.skypro.homework.dto.auth.Role;
import ru.skypro.homework.dto.user.NewPasswordDto;
import ru.skypro.homework.dto.user.UpdateUserDto;
import ru.skypro.homework.dto.user.UserDto;
import ru.skypro.homework.model.UsersDao;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;
import ru.skypro.homework.service.ImageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private ImageService imageService;  // Мокаем ImageService для избежания реальных операций с файлами в CI

    private UsersDao testUser;
    private String userPassword = "password";

    @BeforeEach
    void setUp() {
        testUser = new UsersDao();
        testUser.setEmail("testuser@example.com");
        testUser.setPassword(passwordEncoder.encode(userPassword));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhone("+7 (999) 123-45-67");
        testUser.setRole(Role.USER);
        userRepository.save(testUser);

        // Настраиваем мок для методов ImageService
        Mockito.doNothing().when(imageService).deleteImage(ArgumentMatchers.anyString(), ArgumentMatchers.any());
        Mockito.when(imageService.saveAvatar(ArgumentMatchers.any(MultipartFile.class), ArgumentMatchers.anyInt(), ArgumentMatchers.anyString()))
               .thenReturn("/avatars/mock-avatar.jpg");  // Возвращаем моковый путь
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void getUser_ShouldReturnUserDto() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(testUser.getEmail(), userPassword);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<UserDto> response = restTemplate.exchange(
                baseUrl() + "/users/me",
                HttpMethod.GET,
                requestEntity,
                UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void updateUser_ShouldUpdateUserInfo() {
        UpdateUserDto update = new UpdateUserDto();
        update.setFirstName("Updated");
        update.setLastName("User");
        update.setPhone("+7 (999) 987-65-43");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(testUser.getEmail(), userPassword);
        HttpEntity<UpdateUserDto> requestEntity = new HttpEntity<>(update, headers);

        ResponseEntity<UpdateUserDto> response = restTemplate.exchange(
                baseUrl() + "/users/me",
                HttpMethod.PATCH,
                requestEntity,
                UpdateUserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getFirstName()).isEqualTo("Updated");

        UsersDao updatedUser = userRepository.findByEmail(testUser.getEmail()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
    }

    @Test
    void setPassword_ShouldChangePassword() {
        NewPasswordDto passwordDto = new NewPasswordDto();
        passwordDto.setCurrentPassword(userPassword);
        passwordDto.setNewPassword("newpassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(testUser.getEmail(), userPassword);
        HttpEntity<NewPasswordDto> requestEntity = new HttpEntity<>(passwordDto, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/users/set_password",
                HttpMethod.POST,
                requestEntity,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Проверяем, что новый пароль работает
        headers.setBasicAuth(testUser.getEmail(), "newpassword");
        ResponseEntity<UserDto> authResponse = restTemplate.exchange(
                baseUrl() + "/users/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserDto.class);

        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateUserImage_ShouldUpdateAvatar() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBasicAuth(testUser.getEmail(), userPassword);

        ByteArrayResource imagePart = new ByteArrayResource("new avatar".getBytes()) {
            @Override
            public String getFilename() {
                return "avatar.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imagePart);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<UserDto> response = restTemplate.exchange(
                baseUrl() + "/users/me/image",
                HttpMethod.PATCH,
                requestEntity,
                UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        UsersDao updatedUser = userRepository.findByEmail(testUser.getEmail()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getImage()).isEqualTo("/avatars/mock-avatar.jpg");  // Проверяем моковый путь
    }

    @Test
    void getUser_ShouldReturn401_WhenUnauthorized() {
        ResponseEntity<UserDto> response = restTemplate.getForEntity(
                baseUrl() + "/users/me", UserDto.class);

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