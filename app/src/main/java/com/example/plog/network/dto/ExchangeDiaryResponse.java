package com.example.plog.network.dto;

public class ExchangeDiaryResponse {
    private boolean success;
    private Data data;

    public Long getId() { return data != null ? data.id : null; }
    public Long getSessionId() { return data != null ? data.sessionId : null; }
    public Long getUserId() { return data != null ? data.userId : null; }
    public String getTitle() { return data != null ? data.title : null; }
    public String getContent() { return data != null ? data.content : null; }
    public String getCreatedAt() { return data != null ? data.createdAt : null; }
    public int getDayNumber() { return data != null ? data.dayNumber : 0; }

    public static class Data {
        public Long id;
        public Long sessionId;
        public Long userId;
        public String title;
        public String content;
        public String createdAt;
        public int dayNumber;
    }
}