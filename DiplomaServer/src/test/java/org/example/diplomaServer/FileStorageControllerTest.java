package org.example.diplomaServer;

import org.example.diplomaServer.controller.FileStorageController;
import org.example.diplomaServer.model.AuthTokenInfo;
import org.example.diplomaServer.model.FileInfo;
import org.example.diplomaServer.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class FileStorageControllerTest {


    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileStorageController fileStorageController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAuth_Success() {
        // Arrange
        String login = "testUser";
        String password = "testPass";
        AuthTokenInfo mockTokenInfo = new AuthTokenInfo("validAuthToken");

        // Act
        when(fileStorageService.getAuth(login, password)).thenReturn(mockTokenInfo);
        ResponseEntity<List<AuthTokenInfo>> response = fileStorageController.getAuth(login, password);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(mockTokenInfo.getToken(), response.getBody().get(0).getToken());
    }
    @Test
    void testGetAuth_Unauthorized() {
        // Arrange
        String login = "testUser";
        String password = "wrongPass";

        // Act
        when(fileStorageService.getAuth(login, password)).thenReturn(null);
        ResponseEntity<List<AuthTokenInfo>> response = fileStorageController.getAuth(login, password);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getBody());
    }

    @Test
    void testUploadFile_Success() throws IOException {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        String filePath = "uploads/testFile.txt";
        String token = "testToken";
        when(fileStorageService.saveFile(mockFile, token)).thenReturn(filePath);

        // Act
        ResponseEntity<String> response = fileStorageController.uploadFile(mockFile, token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("File uploaded: " + filePath, response.getBody());
    }

    @Test
    void testGetFile_FileNotFound() throws IOException {
        // Arrange
        String fileName = "nonExistentFile.txt";
        String auth_token = "valid-auth-token";
        File mockFile = mock(File.class);

        when(fileStorageService.getFile(fileName, auth_token)).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(false);

        // Act
        ResponseEntity<byte[]> response = fileStorageController.getFile(fileName, auth_token);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(null, response.getBody());
    }

    @Test
    void testGetFile_InvalidAuthToken() throws IOException {
        // Arrange
        String fileName = "testFile.txt";
        String auth_token = "invalid-auth-token";

        when(fileStorageService.getFile(fileName, auth_token)).thenThrow(new IOException("auth-token does not exist"));

        // Act & Assert
//        try {
//            fileStorageController.getFile(fileName, auth_token);
//        } catch (IOException e) {
//            assertEquals("auth-token does not exist", e.getMessage());
//        }
    }


    @Test
    void testDeleteFile_Success() throws IOException {
        String fileName = "testFile.txt";
        String authToken = "validToken";
        File mockFile = mock(File.class);
        when(fileStorageService.deleteFile(fileName, authToken)).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.delete()).thenReturn(true);

        ResponseEntity<String> response = fileStorageController.deleteFile(fileName, authToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Файл успешно удален: " + fileName, response.getBody());
    }

    @Test
    void testDeleteFile_NotFound() throws IOException {
        String fileName = "nonExistentFile.txt";
        String authToken = "validToken";
        File mockFile = mock(File.class);
        when(fileStorageService.deleteFile(fileName, authToken)).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(false);

        ResponseEntity<String> response = fileStorageController.deleteFile(fileName, authToken);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Файл не найден: " + fileName, response.getBody());
    }

    @Test
    void testDeleteFile_InternalServerError() throws IOException {
        String fileName = "testFile.txt";
        String authToken = "validToken";
        File mockFile = mock(File.class);
        when(fileStorageService.deleteFile(fileName, authToken)).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.delete()).thenReturn(false);

        ResponseEntity<String> response = fileStorageController.deleteFile(fileName, authToken);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ошибка при удалении файла: " + fileName, response.getBody());
    }

    @Test
    void testUpdateFile_Success() throws IOException { // не проходит, улетаем с ошибкой при первом when(
        String oldFileName = "oldFile.txt";
        String newFileName = "newFile.txt";
        String authToken = "validToken";
        String uploadDir = "uploads/";

        when(fileStorageService.updateFile(authToken)).thenReturn(uploadDir);
        File oldFile = new File(uploadDir, oldFileName);
        File newFile = new File(uploadDir, newFileName);
        when(fileStorageService.fileExists(oldFile)).thenReturn(true);
        when(fileStorageService.renameFile(oldFile, newFile)).thenReturn(true);

        ResponseEntity<String> response = fileStorageController.updateFile(oldFileName, newFileName, authToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Имя файла успешно изменено на: " + newFileName, response.getBody());
    }

    @Test
    void testUpdateFile_NotFound() throws IOException { // не работает, вылетаем на первом when()
        String oldFileName = "oldFile.txt";
        String newFileName = "newFile.txt";
        String authToken = "validToken";
        String uploadDir = "uploads/";

        when(fileStorageService.updateFile(authToken)).thenReturn(uploadDir);
        File oldFile = new File(uploadDir, oldFileName);
        when(fileStorageService.fileExists(oldFile)).thenReturn(false);

        ResponseEntity<String> response = fileStorageController.updateFile(oldFileName, newFileName, authToken);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Файл не найден: " + oldFileName, response.getBody());
    }

    @Test
    void testUpdateFile_InternalServerError() throws IOException { //не работает, вылетаем на первом when()
        String oldFileName = "oldFile.txt";
        String newFileName = "newFile.txt";
        String authToken = "validToken";
        String uploadDir = "uploads/";

        when(fileStorageService.updateFile(authToken)).thenReturn(uploadDir);
        File oldFile = new File(uploadDir, oldFileName);
        when(fileStorageService.fileExists(oldFile)).thenReturn(true);
        when(fileStorageService.renameFile(any(), any())).thenReturn(false);

        ResponseEntity<String> response = fileStorageController.updateFile(oldFileName, newFileName, authToken);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ошибка при изменении имени файла: " + oldFileName, response.getBody());
    }

    @Test
    void testGetList_Success() throws IOException {
        String authToken = "validToken";
        File[] mockFiles = new File[]{
                new File("uploads/file1.txt"),
                new File("uploads/file2.txt")
        };

        when(fileStorageService.getList(authToken)).thenReturn(mockFiles);

        ResponseEntity<List<FileInfo>> response = fileStorageController.getList(authToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("file1.txt", response.getBody().get(0).getFilename());
        assertEquals("file2.txt", response.getBody().get(1).getFilename());
    }

    @Test
    void testGetList_InternalServerError() throws IOException {
        String authToken = "validToken";
        when(fileStorageService.getList(authToken)).thenReturn(null);

        ResponseEntity<List<FileInfo>> response = fileStorageController.getList(authToken);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

}