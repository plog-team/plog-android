package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InterruptPayload {

    @SerializedName("diaryId")
    public long diaryId;

    // "2026-05-20"
    @SerializedName("date")
    public String date;

    // "N월 NN일 일기의 '~~' 부분이 슬펐다는 내용이 맞나요?"
    @SerializedName("question")
    public String question;

    // ["맞아요", "아니요, 기뻤어요", "그냥 넘어가도 돼요"]
    @SerializedName("options")
    public List<String> options;
}
