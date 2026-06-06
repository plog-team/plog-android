package com.example.plog.api.model;

public class ClickLogRequest {
    public String contentId;
    public String contentTypeId;
    public String category;

    public ClickLogRequest(String contentId, String contentTypeId, String category) {
        this.contentId     = contentId;
        this.contentTypeId = contentTypeId;
        this.category      = category;
    }
}