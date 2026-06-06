package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DiarySimpleResponse {

    @SerializedName("diaryId")
    public long diaryId;

    @SerializedName("date")
    public String date;

    @SerializedName("title")
    public String title;

    @SerializedName("body")
    public String body;

    @SerializedName("location")
    public String location;

    @SerializedName("weather")
    public String weather;

    @SerializedName("secret")
    public boolean secret;

    @SerializedName("bookmarked")
    public boolean bookmarked;

    @SerializedName("representativePhotoIndex")
    public int representativePhotoIndex;

    @SerializedName("photoIds")
    public List<Long> photoIds;
}
