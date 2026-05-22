package com.example.plog.search;

public class SearchDiary {

    private String date;
    private String emotion;
    private String title;
    private String content;
    private String location;
    private String imageUrl;

    public SearchDiary(String date, String emotion, String title,
                       String content, String location, String imageUrl) {
        this.date = date;
        this.emotion = emotion;
        this.title = title;
        this.content = content;
        this.location = location;
        this.imageUrl = imageUrl;
    }

    public String getDate() { return date; }
    public String getEmotion() { return emotion; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
}