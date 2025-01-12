package org.example.diplomaServer.controller;

import org.example.diplomaServer.model.AuthTokenInfo;
import org.example.diplomaServer.model.FileInfo;
import org.example.diplomaServer.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @Autowired
    public FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/login")
    public ResponseEntity<List<AuthTokenInfo>> getAuth(@RequestParam String login, @RequestParam String password) {
            AuthTokenInfo authTokenInfo = fileStorageService.getAuth(login, password);
        if (authTokenInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        } else {
            System.out.println(authTokenInfo.getToken());
            return ResponseEntity.ok(List.of(authTokenInfo));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(value = "auth-token", required = false) String auth_token) {
        if(auth_token == null || auth_token.isEmpty()) {
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Токен не передан!");
        }
        try {
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(fileStorageService.deleteAuthToken(auth_token));
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Неверный токен авторизации");
        }
    }

    @PostMapping("/file")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestHeader(value = "auth-token", required = false) String auth_token) {
        if(auth_token == null) {
            return ResponseEntity.status(403).body("Auth-token is null!");
        }
        try {
            String path = fileStorageService.saveFile(file, auth_token);
            return ResponseEntity.ok("File uploaded: " + path);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("File upload failed");
        }
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> getFile(@RequestParam String fileName, @RequestHeader("auth-token") String auth_token) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        try {
            File file = fileStorageService.getFile(fileName, auth_token);

            if (!fileStorageService.fileExists(file)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(file.getName())
                    .build());

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("auth-token does not exist")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<String> deleteFile(@RequestParam String fileName, @RequestHeader("auth-token") String auth_token) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Файл не найден: " + fileName);
        }

        try {
            File file = fileStorageService.deleteFile(fileName, auth_token);
            if (!file.exists()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Файл не найден: " + fileName);
            }

            if (file.delete()) {
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Файл успешно удален: " + fileName);
            } else {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Ошибка при удалении файла: " + fileName);
            }
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("auth-token does not exist")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Неверный токен авторизации");
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Ошибка при удалении файла: " + fileName);
        }
    }

    @PutMapping("/file")
    public ResponseEntity<String> updateFile(@RequestParam String fileName,
                                             @RequestParam String newFileName,
                                             @RequestHeader("auth-token") String auth_token) {
        // Проверяем, что имена файлов не пустые
        if (fileName == null || fileName.trim().isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Файл не найден: " + fileName);
        }
        if (newFileName == null || newFileName.trim().isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Новое имя файла не может быть пустым");
        }

        try {
            String uploadDir = fileStorageService.updateFile(auth_token);
            File oldFile = new File(uploadDir, fileName);
            File newFile = new File(uploadDir, newFileName);

            if (!fileStorageService.fileExists(oldFile)) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Файл не найден: " + fileName);
            }

            if (fileStorageService.renameFile(oldFile, newFile)) {
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Имя файла успешно изменено на: " + newFileName);
            } else {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Ошибка при изменении имени файла: " + fileName);
            }
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("auth-token does not exist")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Неверный токен авторизации");
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Ошибка при изменении имени файла: " + fileName);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfo>> getList(@RequestHeader("auth-token") String auth_token) {
        try {
            File[] files = fileStorageService.getList(auth_token);
            if (files == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            List<FileInfo> fileInfoList = new ArrayList<>();
            for (int i = 0; i < Math.min(files.length, 10); i++) {
                File file = files[i];
                fileInfoList.add(new FileInfo(file.getName(), file.length()));
            }
            return ResponseEntity.ok(fileInfoList);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("auth-token does not exist")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
