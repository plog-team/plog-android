package com.example.plog.network.dto;

public class ExchangeRoomResponse {
    private boolean success;
    private Data data;

    public Long getId() { return data != null ? data.id : null; }
    public Long getMatchId() { return data != null ? data.matchId : null; }
    public String getStatus() { return data != null ? data.status : null; }
    public String getCreatedAt() { return data != null ? data.createdAt : null; }

    public static class Data {
        private Long id;
        private Long matchId;
        private String status;
        private String createdAt;
    }
}