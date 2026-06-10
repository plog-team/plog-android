package com.example.plog.network.auth;

public class LoginRequest {
    private String nickname;
    private String userPassword;

    public LoginRequest(String nickname, String userPassword) {
        this.nickname = nickname;
        this.userPassword = userPassword;
    }
}
