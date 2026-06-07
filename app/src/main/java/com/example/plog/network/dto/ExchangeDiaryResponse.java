package com.example.plog.network.dto;

public class ExchangeDiaryResponse {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String content;
    private String createdAt;
    private int dayNumber;

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getUserId() { return userId; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public int getDayNumber() { return dayNumber; }
}