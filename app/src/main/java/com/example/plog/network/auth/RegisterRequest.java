package com.example.plog.network.auth;

public class RegisterRequest {

    private String userName;
    private String email;
    private String userPassword;

    public RegisterRequest(
            String userName,
            String email,
            String userPassword
    ) {
        this.userName = userName;
        this.email = email;
        this.userPassword = userPassword;
    }
}