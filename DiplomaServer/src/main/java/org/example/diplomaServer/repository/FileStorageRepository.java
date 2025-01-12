package org.example.diplomaServer.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.diplomaServer.model.AuthTokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;


import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FileStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageRepository.class);
    private List<AuthTokenInfo> listOfAuthTokens = new ArrayList<AuthTokenInfo>();

    @PersistenceContext
    EntityManager entityManager;

    public AuthTokenInfo getAuth(String login, String password) {
        String[] info = {"", ""};
        List<Object[]> accounts = entityManager.createNativeQuery("SELECT * FROM accounts WHERE login = :login AND password = :password")
                .setParameter("login", login)
                .setParameter("password", password)
                .getResultList();

        accounts.forEach(row -> {
            info[0] += row[1];
            info[1] += row[2];
            logger.info("login: " + info[0] + ", password: " + info[1]);
        });

        if (info[0].isEmpty() || info[1].isEmpty()) {
            return null; // Вернуть 401, если нет авторизации
        } else {
            AuthTokenInfo authTokenInfo = generateAuthToken();
            logger.info("auth-token generated: {}", authTokenInfo.getToken());
            return authTokenInfo;
        }
    }

    public String deleteAuthToken(String auth_token) throws IOException {
        if(!findSimilarAuthToken(auth_token)) {
            throw new IOException("auth-token does not exist");
        }
        AuthTokenInfo authToken = null;
        for(AuthTokenInfo authTokenInfo : listOfAuthTokens){
            if(authTokenInfo.getToken().equals(auth_token)){
                authToken = authTokenInfo;
            }
        }
        var result = listOfAuthTokens.remove(authToken);
        logger.info("Актуальные токены: " + listOfAuthTokens.toString());

        if(result){
            return "token " + auth_token +  " was deleted";
        }else {
            return "error 401";
        }
    }


    private AuthTokenInfo generateAuthToken() {
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final int LENGTH = 14;
        final SecureRandom random = new SecureRandom();

        StringBuilder result = new StringBuilder(LENGTH); //генерируем auth_token из букв и цифр
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(index));
        }

        if(findSimilarAuthToken(result.toString())){
            generateAuthToken();
        }
        listOfAuthTokens.add(new AuthTokenInfo(result.toString()));
        return new AuthTokenInfo(result.toString());
    }

    public boolean findSimilarAuthToken(String auth_token) {
        for(AuthTokenInfo authTokenInfo : listOfAuthTokens){
            if(authTokenInfo.getToken().equals(auth_token)){
                return true;
            }
        }
        return false;
    }
    public void saveAccount(String login, String password) {
        entityManager.createNativeQuery("INSERT INTO accounts (id,login,password) VALUES (2L, login = :login, password = :password)")
                .setParameter("login", login)
                .setParameter("password", password);
    }
}
