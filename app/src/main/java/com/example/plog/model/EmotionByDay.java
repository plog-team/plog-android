package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class EmotionByDay {

    // "2026-05-20"
    @SerializedName("date")
    public String date;

    // "월" | "화" | "수" | "목" | "금" | "토" | "일"
    @SerializedName("day")
    public String day;

    // null이면 해당 날 일기 없음
    @SerializedName("emotion")
    public String emotion;
}
