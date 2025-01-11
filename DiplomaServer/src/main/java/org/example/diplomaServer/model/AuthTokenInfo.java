package org.example.diplomaServer.model;

public class AuthTokenInfo {
    private String token;
    public AuthTokenInfo(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
}
