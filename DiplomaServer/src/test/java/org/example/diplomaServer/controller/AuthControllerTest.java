package org.example.diplomaServer.controller;

import org.example.diplomaServer.model.AuthTokenInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
public class AuthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static {
        postgresContainer.start();
    }

    @BeforeEach
    void setUp() {
        // Здесь вы можете подготовить тестовые данные
        prepareTestData();
    }

    private void prepareTestData() {
        // Здесь вы добавите код для вставки тестовых данных в таблицу "accounts".
        // Например, использовать JdbcTemplate или EntityManager для выполнения SQL.
    }

    @Test
    void testSuccessfulLogin() {
        // Задайте логин и пароль, соответствующие данным в тестовой базе данных.
        String login = "testUser";
        String password = "testPass";

        ResponseEntity<List<AuthTokenInfo>> response = restTemplate.postForEntity(
                "/login?login=" + login + "&password=" + password, null, new ParameterizedTypeReference<List<AuthTokenInfo>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getToken()).isNotNull(); // Проверка наличия токена
    }

    @Test
    void testUnsuccessfulLogin() {
        String login = "wrongUser";
        String password = "wrongPass";

        ResponseEntity<List<AuthTokenInfo>> response = restTemplate.postForEntity(
                "/login?login=" + login + "&password=" + password, null, new ParameterizedTypeReference<List<AuthTokenInfo>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull(); // Тело ответа должно быть null
    }
}

