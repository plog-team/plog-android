package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class ClarifyRequest {

    @SerializedName("answer")
    public String answer;

    public ClarifyRequest(String answer) {
        this.answer = answer;
    }
}
