package org.example.diplomaServer.controller;

import org.example.diplomaServer.model.Accounts;
import org.example.diplomaServer.repository.FileStorageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class FileStorageControllerTestContainers {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FileStorageRepository fileStorageRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    @Rollback(false)
    void setUp() {
        // Очищаем таблицу accounts перед каждым тестом
        entityManager.createNativeQuery("DELETE FROM accounts").executeUpdate();

        // Создаем тестовый аккаунт
        Accounts testAccount = Accounts.builder()
                .id("1")
                .login("testUser")
                .password("testPassword")
                .build();

        entityManager.persist(testAccount);
        entityManager.flush();
    }

    @Test
    void loginSuccess() throws Exception {
        // Выполняем POST запрос на /login с правильными учетными данными
        mockMvc.perform(post("/login")
                        .param("login", "testUser")
                        .param("password", "testPassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].token").exists())
                .andExpect(jsonPath("$[0].token").isString());
    }

    @Test
    void loginFailure() throws Exception {
        // Проверяем неверные учетные данные
        mockMvc.perform(post("/login")
                        .param("login", "wrongUser")
                        .param("password", "wrongPassword"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutSuccess() throws Exception {
        // Сначала получаем валидный токен через логин
        String authToken = getAuthToken();

        // Выполняем запрос на logout с полученным токеном
        mockMvc.perform(post("/logout")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("token " + authToken + " was deleted")));
    }

    @Test
    void logoutWithoutToken() throws Exception {
        // Пробуем выполнить logout без токена
        mockMvc.perform(post("/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Токен не передан!"));
    }

    @Test
    void logoutWithInvalidToken() throws Exception {
        // Пробуем выполнить logout с невалидным токеном
        mockMvc.perform(post("/logout")
                        .header("auth-token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Неверный токен авторизации"));
    }
    @Test
    void uploadFileSuccess() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Создаем тестовый файл
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        // Выполняем запрос на загрузку файла
        mockMvc.perform(multipart("/file")
                        .file(file)
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("File uploaded:")));
    }

    @Test
    void uploadFileWithoutToken() throws Exception {
        // Создаем тестовый файл
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        // Пытаемся загрузить файл без токена
        mockMvc.perform(multipart("/file")
                        .file(file))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Auth-token is null!"));
    }

    @Test
    void uploadFileWithInvalidToken() throws Exception {
        // Создаем тестовый файл
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );

        // Пытаемся загрузить файл с невалидным токеном
        mockMvc.perform(multipart("/file")
                        .file(file)
                        .header("auth-token", "invalid-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File upload failed"));
    }

    @Test
    void uploadFileWithEmptyFile() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Создаем пустой файл
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        // Пытаемся загрузить пустой файл
        mockMvc.perform(multipart("/file")
                        .file(file)
                        .header("auth-token", authToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File upload failed"));
    }

    private void createTestFile(String authToken, String fileName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes()
        );

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .header("auth-token", authToken))
                .andExpect(status().isOk());
    }

    @Test
    void getFileSuccess() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Сначала создаем тестовый файл
        String fileName = "test-get.txt";
        createTestFile(authToken, fileName);

        // Пытаемся получить созданный файл
        mockMvc.perform(get("/file")
                        .param("fileName", fileName)
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(fileName)));
    }

    @Test
    void getFileWithoutToken() throws Exception {
        // Пытаемся получить файл без токена
        mockMvc.perform(get("/file")
                        .param("fileName", "any-file.txt"))
                .andExpect(status().isBadRequest()); // Spring вернет 400, так как токен обязателен
    }

    @Test
    void getFileWithInvalidToken() throws Exception {
        // Пытаемся получить файл с невалидным токеном
        mockMvc.perform(get("/file")
                        .param("fileName", "any-file.txt")
                        .header("auth-token", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFileNotFound() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Пытаемся получить несуществующий файл
        mockMvc.perform(get("/file")
                        .param("fileName", "non-existent-file.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFileWithEmptyFileName() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Пытаемся получить файл с пустым именем
        mockMvc.perform(get("/file")
                        .param("fileName", "")
                        .header("auth-token", authToken))
                .andExpect(status().isNotFound());
    }

    private String getAuthToken() throws Exception {
        // Выполняем логин и получаем токен
        String response = mockMvc.perform(post("/login")
                        .param("login", "testUser")
                        .param("password", "testPassword"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Извлекаем токен из JSON ответа (используем простую строковую обработку)
        return response.split("\"token\":\"")[1].split("\"")[0];
    }
    @Test
    void deleteFileSuccess() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Сначала создаем тестовый файл
        String fileName = "test-delete.txt";
        createTestFile(authToken, fileName);

        // Пытаемся удалить созданный файл
        mockMvc.perform(delete("/file")
                        .param("fileName", fileName)
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Файл успешно удален: " + fileName));
    }

    @Test
    void deleteFileWithoutToken() throws Exception {
        // Пытаемся удалить файл без токена
        mockMvc.perform(delete("/file")
                        .param("fileName", "any-file.txt"))
                .andExpect(status().isBadRequest()); // Spring вернет 400, так как токен обязателен
    }

    @Test
    void deleteFileWithInvalidToken() throws Exception {
        // Пытаемся удалить файл с невалидным токеном
        mockMvc.perform(delete("/file")
                        .param("fileName", "any-file.txt")
                        .header("auth-token", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteFileNotFound() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Пытаемся удалить несуществующий файл
        mockMvc.perform(delete("/file")
                        .param("fileName", "non-existent-file.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Файл не найден: non-existent-file.txt"));
    }

    @Test
    void deleteFileWithEmptyFileName() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Пытаемся удалить файл с пустым именем
        mockMvc.perform(delete("/file")
                        .param("fileName", "")
                        .header("auth-token", authToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Файл не найден: "));
    }

    @Test
    void updateFileWithoutToken() throws Exception {
        // Пытаемся переименовать файл без токена
        mockMvc.perform(put("/file")
                        .param("fileName", "old.txt")
                        .param("newFileName", "new.txt"))
                .andExpect(status().isBadRequest()); // Spring вернет 400, так как токен обязателен
    }

    @Test
    void updateFileWithInvalidToken() throws Exception {
        // Пытаемся переименовать файл с невалидным токеном
        mockMvc.perform(put("/file")
                        .param("fileName", "old.txt")
                        .param("newFileName", "new.txt")
                        .header("auth-token", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateFileNotFound() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Пытаемся переименовать несуществующий файл
        mockMvc.perform(put("/file")
                        .param("fileName", "non-existent.txt")
                        .param("newFileName", "new.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Файл не найден: non-existent.txt"));
    }

    @Test
    void updateFileWithEmptyFileName() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Пытаемся переименовать файл с пустым именем
        mockMvc.perform(put("/file")
                        .param("fileName", "")
                        .param("newFileName", "new.txt")
                        .header("auth-token", authToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Файл не найден: "));
    }

    @Test
    void updateFileWithEmptyNewFileName() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Создаем тестовый файл
        String fileName = "test-empty-new.txt";
        createTestFile(authToken, fileName);

        // Пытаемся переименовать файл с пустым новым именем
        mockMvc.perform(put("/file")
                        .param("fileName", fileName)
                        .param("newFileName", "")
                        .header("auth-token", authToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Новое имя файла не может быть пустым"));
    }
    @Test
    void getListSuccess() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Создаем несколько тестовых файлов
        createTestFile(authToken, "test1.txt");
        createTestFile(authToken, "test2.txt");
        createTestFile(authToken, "test3.txt");

        // Получаем список файлов
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].filename").exists())
                .andExpect(jsonPath("$[0].size").exists())
                .andExpect(jsonPath("$[*].filename", hasItems("test1.txt", "test2.txt", "test3.txt")));
    }

    @Test
    void getListWithoutToken() throws Exception {
        // Пытаемся получить список файлов без токена
        mockMvc.perform(get("/list"))
                .andExpect(status().isBadRequest()); // Spring вернет 400, так как токен обязателен
    }

    @Test
    void getListWithManyFiles() throws Exception {
        // Получаем валидный токен
        String authToken = getAuthToken();

        // Создаем больше 10 файлов
        for (int i = 1; i <= 12; i++) {
            createTestFile(authToken, "test" + i + ".txt");
        }

        // Проверяем, что возвращается только первые 10 файлов
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(10))); // Проверяем ограничение на 10 файлов
    }
}
