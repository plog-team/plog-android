package com.example.plog.ui.recommend.model;

public class PlaceItem {
    private String contentId, title, address,
            imageUrl, category, contentTypeId,
            distance, congestion;
    private double latitude, longitude;
    private int congestionScore = 0;

    public PlaceItem(String contentId, String title, String address,
                     String imageUrl, String category,
                     String contentTypeId, String distance,
                     double latitude, double longitude) {
        this.contentId     = contentId;
        this.title         = title;
        this.address       = address;
        this.imageUrl      = imageUrl;
        this.category      = category;
        this.contentTypeId = contentTypeId;
        this.distance      = distance;
        this.congestion    = "정보없음";
        this.latitude      = latitude;
        this.longitude     = longitude;
    }

    public String getContentId()     { return contentId; }
    public String getTitle()         { return title; }
    public String getAddress()       { return address; }
    public String getImageUrl()      { return imageUrl; }
    public String getCategory()      { return category; }
    public String getContentTypeId() { return contentTypeId; }
    public String getDistance()      { return distance; }
    public double getLatitude()      { return latitude; }
    public double getLongitude()     { return longitude; }
    public String getCongestion()    { return congestion; }
    public int getCongestionScore()  { return congestionScore; }

    public void setCongestion(String congestion) {
        this.congestion = congestion;
    }
    public void setCongestionScore(int score) {
        this.congestionScore = score;
    }
}