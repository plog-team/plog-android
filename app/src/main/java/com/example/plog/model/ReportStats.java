package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class ReportStats {

    @SerializedName("primaryEmotion")
    public String primaryEmotion;

    // e.g. {"기쁨": 3, "슬픔": 1, "설렘": 2}
    @SerializedName("emotionFrequency")
    public Map<String, Integer> emotionFrequency;

    @SerializedName("emotionByDay")
    public List<EmotionByDay> emotionByDay;

    @SerializedName("topPlaces")
    public List<PlaceEntry> topPlaces;
}
