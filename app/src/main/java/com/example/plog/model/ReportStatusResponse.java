package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class ReportStatusResponse {

    // "running" | "interrupted" | "done" | "error"
    @SerializedName("status")
    public String status;

    @SerializedName("threadId")
    public String threadId;

    // 리포트 제목에 표시할 사용자명 (null이면 "사용자" 폴백)
    @SerializedName("userName")
    public String userName;

    // 월간 장소 분석 (status == "done"일 때 채워짐)
    @SerializedName("placeReport")
    public PlaceReportData placeReport;

    // 주간 감정 분석 (status == "done"일 때 채워짐)
    @SerializedName("emotionReport")
    public EmotionReportData emotionReport;

    // LangGraph Interrupt 발생 시 채워짐
    @SerializedName("interruptPayload")
    public InterruptPayload interruptPayload;

    // status == "error"일 때 원인 메시지
    @SerializedName("message")
    public String message;
}
