package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GuideQuestionDto {

    @SerializedName("questionId")
    public Long questionId;

    @SerializedName("orderIdx")
    public Integer orderIdx;

    @SerializedName("question")
    public String question;

    @SerializedName("type")
    public String type;

    /** [Day 8.11] Gemini가 제안한 답변 후보 3개 — Chip UX */
    @SerializedName("suggestedAnswers")
    public List<String> suggestedAnswers;

    @SerializedName("answer")
    public String answer;
}
