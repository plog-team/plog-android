package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EmotionByDay {

    @SerializedName("date")
    public String date;

    @SerializedName("day")
    public String day;

    // null 또는 빈 리스트면 해당 날 감정 기록 없음
    @SerializedName("emotions")
    public List<String> emotions;

    // true면 일기가 존재하는 날, false면 일기 자체가 없는 날
    @SerializedName("hasDiary")
    public boolean hasDiary;
}
