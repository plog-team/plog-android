package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VisionResult {

    @SerializedName("objects")
    public List<String> objects;

    @SerializedName("scene")
    public String scene;

    @SerializedName("mood")
    public String mood;

    @SerializedName("time_of_day")
    public String timeOfDay;

    @SerializedName("weather_hint")
    public String weatherHint;

    @SerializedName("suggested_emotion")
    public String suggestedEmotion;

    @SerializedName("one_line_summary")
    public String oneLineSummary;
}
