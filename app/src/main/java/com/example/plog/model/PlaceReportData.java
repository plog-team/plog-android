package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlaceReportData {

    // "2026년 5월"
    @SerializedName("period")
    public String period;

    // AI 생성 장소 분석 텍스트
    @SerializedName("content")
    public String content;

    // 가장 자주 방문한 장소의 일기 대표 사진 URL (null이면 사진 없음)
    @SerializedName("topPhotoUrl")
    public String topPhotoUrl;

    // 방문 장소 목록 (빈도 내림차순)
    @SerializedName("places")
    public List<PlaceEntry> places;
}
