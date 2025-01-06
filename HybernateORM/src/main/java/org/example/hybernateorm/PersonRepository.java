package org.example.hybernateorm;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PersonRepository {

    private List<String> listOfAuthTokens = new ArrayList<String>();
    private final String uploadDir = "uploads/";

    @PersistenceContext
    EntityManager entityManager;

    public JSONObject getAuth(String login, String password) {
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
            return json;
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

    private String generateAuthToken(){
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
