package com.example.plog.network.dto;

public class ExchangeRoomResponse {
    private Long id;
    private Long matchId;
    private String status;
    private String createdAt;

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}