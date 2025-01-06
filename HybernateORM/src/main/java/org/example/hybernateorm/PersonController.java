package org.example.hybernateorm;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class PersonController {
    private final PersonRepository personRepository;

    @Autowired
    public PersonController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @PostMapping("/login")
    public JSONObject authentification(@RequestParam String login, @RequestParam String password) {
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
    public String getFiles(@RequestParam MultipartFile file) {return file.getOriginalFilename();}

    @DeleteMapping("/file")
    public String deleteFile(@RequestParam MultipartFile file) {return file.getOriginalFilename();}

    @PutMapping("/file")
    public String updateFile(@RequestParam MultipartFile file) {return file.getOriginalFilename();}

    @GetMapping("/list")
    public String getList(@RequestParam int page, @RequestParam int rows) {
        return "";
    }
}
