package com.example.plog.network.auth;

public class LoginResponse {
    private String token;
    private int userId;
    private String email;

    public String getToken() { return token; }
    public int getUserId() { return userId; }
    public String getEmail() { return email; }

    private boolean success;
    private Data data;
    private String error;

    public Data getData() { return data; }

    public static class Data {
        private String accessToken;
        public String getAccessToken() { return accessToken; }
    }
}