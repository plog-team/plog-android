package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class AnswerResponse {

    @SerializedName("answered")
    public GuideQuestionDto answered;

    @SerializedName("nextQuestion")
    public GuideQuestionDto nextQuestion;

    @SerializedName("done")
    public boolean done;

    @SerializedName("answeredCount")
    public int answeredCount;

    @SerializedName("targetCount")
    public int targetCount;
}
