package com.example.plog.network.dto;

public class NotificationResponse {
    private Long id;
    private String type;
    private Long targetId;
    private String targetType;
    private String createdAt;
    private boolean isRead;
    private String message;

    public Long getId() { return id; }
    public String getType() { return type; }
    public Long getTargetId() { return targetId; }
    public String getTargetType() { return targetType; }
    public String getCreatedAt() { return createdAt; }
    public boolean isRead() { return isRead; }
    public String getMessage() { return message; }
}