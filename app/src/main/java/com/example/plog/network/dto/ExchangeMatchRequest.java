package com.example.plog.network.dto;

public class ExchangeMatchRequest {
    private Long userId;
    private Long targetUserId;

    public ExchangeMatchRequest(Long userId, Long targetUserId) {
        this.userId = userId;
        this.targetUserId = targetUserId;
    }
}