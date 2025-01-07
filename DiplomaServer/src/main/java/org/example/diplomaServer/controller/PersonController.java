package org.example.diplomaServer.controller;

import org.example.diplomaServer.model.FileInfo;
import org.example.diplomaServer.repository.PersonRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class PersonController {
    private final PersonRepository personRepository;

    @Autowired
    public PersonController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<JSONObject> authentification(@RequestParam String login, @RequestParam String password) {
        return personRepository.getAuth(login, password);
    }

    @PostMapping("/logout")
    public String logout(@RequestHeader String auth_token) {
        if(auth_token == null) {
            return "Токен не передан!";
        }
        return personRepository.deleteAuthToken(auth_token);
    }

    @PostMapping("/file")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestHeader String auth_token) {
        if(auth_token == null) {
            return ResponseEntity.status(403).body("Auth-token is null!");
        }
        try {
            String path = personRepository.saveFile(file, auth_token);
            return ResponseEntity.ok("File uploaded: " + path);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("File upload failed");
        }
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> getFile(@RequestParam String fileName, @RequestHeader String auth_token) throws IOException {
        return personRepository.getFile(fileName, auth_token);
    }

    @DeleteMapping("/file")
    public ResponseEntity<String> deleteFile(@RequestParam String fileName, @RequestHeader String auth_token) throws IOException {
        return personRepository.deleteFile(fileName, auth_token);
    }

    @PutMapping("/file")
    public ResponseEntity<String> updateFile(@RequestParam String fileName, @RequestParam String newFileName, @RequestHeader String auth_token) throws IOException {
        return personRepository.updateFile(fileName, newFileName, auth_token);
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfo>> getList(@RequestHeader String auth_token) throws IOException {
        return personRepository.getList(auth_token);
    }
}
