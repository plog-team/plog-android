package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class PlaceEntry {

    @SerializedName("placeName")
    public String placeName;

    @SerializedName("count")
    public int count;

    // 해당 장소에서 가장 많이 느낀 감정
    @SerializedName("mainEmotion")
    public String mainEmotion;

    // 해당 장소 일기의 대표 사진 URL (null 가능)
    @SerializedName("photoUrl")
    public String photoUrl;
}
