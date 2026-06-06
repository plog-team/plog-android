package com.example.plog.api.model;

public class BookmarkRequest {
    public String contentId;
    public String title;
    public String address;
    public String imageUrl;
    public String category;
    public String contentTypeId;

    public BookmarkRequest(String contentId, String title, String address,
                           String imageUrl, String category, String contentTypeId) {
        this.contentId     = contentId;
        this.title         = title;
        this.address       = address;
        this.imageUrl      = imageUrl;
        this.category      = category;
        this.contentTypeId = contentTypeId;
    }
}
