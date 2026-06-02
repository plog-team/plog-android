package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SessionDetailResponse {

    @SerializedName("sessionId")
    public Long sessionId;

    @SerializedName("userId")
    public Long userId;

    @SerializedName("status")
    public String status;

    @SerializedName("photoIdsCsv")
    public String photoIdsCsv;

    @SerializedName("draft")
    public String draft;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("completedAt")
    public String completedAt;

    @SerializedName("questions")
    public List<GuideQuestionDto> questions;
}
