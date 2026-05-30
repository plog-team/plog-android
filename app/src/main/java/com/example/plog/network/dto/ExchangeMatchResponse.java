package com.example.plog.network.dto;

public class ExchangeMatchResponse {
    private Long id;
    private String status;
    private String createdAt;
    private String requesterNickname;

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getRequesterNickname() { return requesterNickname; }
}