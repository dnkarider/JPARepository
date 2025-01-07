package org.example.diplomaServer.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.diplomaServer.model.FileInfo;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PersonRepository {

    private List<String> listOfAuthTokens = new ArrayList<String>();
    private final String uploadDir = "uploads/";

    @PersistenceContext
    EntityManager entityManager;

    public ResponseEntity<JSONObject> getAuth(String login, String password) {
        final String[] info = {""};
        var accounts = entityManager.createNativeQuery("SELECT * FROM accounts WHERE login = :login AND password = :password").setParameter("login", login).setParameter("password",password).getResultList();
        accounts.forEach(row -> {
            var rowArray = (Object[]) row;
            info[0] += rowArray[1];
            System.out.println(info[0]);
        });
        if (info[0].isEmpty()) {
            return null;
        }
        else{
            String authToken = generateAuthToken();
            listOfAuthTokens.add(authToken);
            JSONObject json = new JSONObject();
            json.put("auth-token", authToken);
            System.out.println(json.toString());
            return ResponseEntity.ok(json);
        }
    }

    public String deleteAuthToken(String auth_token){
        var result = listOfAuthTokens.remove(auth_token);
        System.out.println(listOfAuthTokens.toString());

        if(result){
            return "token was deleted";
        }else {
            return "error 401";
        }
    }

    public String saveFile(MultipartFile file, String auth_token) throws IOException {
        if(!listOfAuthTokens.contains(auth_token)){
            throw new IOException("File not found");
        }
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File absoluteFile = new File(dir.getAbsolutePath());
            File destinationFile = new File(absoluteFile, file.getOriginalFilename());
            file.transferTo(destinationFile);
            return destinationFile.getAbsolutePath();
        }

    public ResponseEntity<byte[]> getFile(String fileName, String auth_token) throws IOException {
        if (!listOfAuthTokens.contains(auth_token)) {
            throw new IOException("auth_token does not exist");
        }
        File file = new File(uploadDir, fileName);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", file.getName());
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<String> deleteFile(String fileName, String auth_token) throws IOException {

        if (!listOfAuthTokens.contains(auth_token)) {
            throw new IOException("auth_token does not exist");
        }
        File file = new File(uploadDir, fileName);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Файл не найден: " + fileName);
        }

        if (file.delete()) {
            return ResponseEntity.ok("Файл успешно удален: " + fileName);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удалении файла: " + fileName);
        }
    }

    public ResponseEntity<String> updateFile(String fileName, String newFileName, String auth_token) throws IOException {

        if (!listOfAuthTokens.contains(auth_token)) {
            throw new IOException("auth_token does not exist");
        }
        File oldFile = new File(uploadDir, fileName);
        if (!oldFile.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Файл не найден: " + fileName);
        }

        File newFile = new File(uploadDir, newFileName);
        if (oldFile.renameTo(newFile)) {
            return ResponseEntity.ok("Имя файла успешно изменено на: " + newFileName);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при изменении имени файла: " + fileName);
        }
    }

    public ResponseEntity<List<FileInfo>> getList(String auth_token) throws IOException {

        if (!listOfAuthTokens.contains(auth_token)) {
            throw new IOException("auth_token does not exist");
        }

        File directory = new File(uploadDir);
        File[] files = directory.listFiles();

        if (files == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        List<FileInfo> fileInfoList = new ArrayList<>();
        for (int i = 0; i < Math.min(files.length, 10); i++) {
            File file = files[i];
            if (file.isFile()) {
                fileInfoList.add(new FileInfo(file.getName(), file.length()));
            }
        }
        System.out.println(fileInfoList);

        return ResponseEntity.ok(fileInfoList);
    }

    private String generateAuthToken() {
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final int LENGTH = 14;
        final SecureRandom random = new SecureRandom();

        StringBuilder result = new StringBuilder(LENGTH); //генерируем auth_token из букв и цифр
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(index));
        }

        boolean findSimilarAuthToken = listOfAuthTokens.contains(result.toString());
        if(findSimilarAuthToken){
            generateAuthToken();
        }
        return result.toString();
    }
}
