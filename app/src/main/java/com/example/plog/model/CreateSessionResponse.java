package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateSessionResponse {

    @SerializedName("sessionId")
    public Long sessionId;

    @SerializedName("status")
    public String status;

    @SerializedName("mode")
    public String mode;

    @SerializedName("questions")
    public List<GuideQuestionDto> questions;

    @SerializedName("firstAssistantMessage")
    public String firstAssistantMessage;

    @SerializedName("photos")
    public List<PhotoAnalysisDto> photos;
}
