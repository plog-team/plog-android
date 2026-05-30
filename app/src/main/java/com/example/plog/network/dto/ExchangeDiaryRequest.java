package com.example.plog.network.dto;

public class ExchangeDiaryRequest {
    private Long sessionId;
    private Long userId;
    private String content;
    private int dayNumber;

    public ExchangeDiaryRequest(Long sessionId, Long userId, String content, int dayNumber) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.content = content;
        this.dayNumber = dayNumber;
    }
}