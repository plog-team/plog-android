package com.example.plog.network.auth;

public class LoginResponse {
    private boolean success;
    private Data data;
    private String error;

    public boolean isSuccess() { return success; }
    public Data getData() { return data; }
    public String getError() { return error; }

    public static class Data {
        private String token;
        private long userId;

        public String getToken() { return token; }
        public long getUserId() { return userId; }
    }
}
