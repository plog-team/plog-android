package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class EmotionReportData {

    // "5월 20일 - 5월 26일"
    @SerializedName("period")
    public String period;

    // AI 생성 감정 분석 텍스트
    @SerializedName("content")
    public String content;

    // 이번 주 주요 감정
    @SerializedName("primaryEmotion")
    public String primaryEmotion;

    // {"기쁨": 3, "슬픔": 1, "설렘": 2}
    @SerializedName("emotionFrequency")
    public Map<String, Integer> emotionFrequency;

    // 요일별 감정 (최근 7일)
    @SerializedName("emotionByDay")
    public List<EmotionByDay> emotionByDay;
}
