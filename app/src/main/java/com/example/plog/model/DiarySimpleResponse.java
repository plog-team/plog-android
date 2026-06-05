package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

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
}
