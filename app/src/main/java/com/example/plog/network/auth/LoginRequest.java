package com.example.plog.network.auth;
public class LoginRequest {
    private String name;
    private String userPassword;

    public LoginRequest(String name, String userPassword) {
        this.name = name;
        this.userPassword = userPassword;
    }
}