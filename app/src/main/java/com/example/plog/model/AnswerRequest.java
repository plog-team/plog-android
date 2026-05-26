package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class AnswerRequest {

    @SerializedName("answer")
    public String answer;

    public AnswerRequest(String answer) {
        this.answer = answer;
    }
}
