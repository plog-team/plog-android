package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class SendChatRequest {

    @SerializedName("message")
    public String message;

    public SendChatRequest(String message) {
        this.message = message;
    }
}
