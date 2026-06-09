package com.example.plog.network.aichat;

import com.google.gson.annotations.SerializedName;

public class AiChatMessageResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public OuterData data;

    public static class OuterData {
        @SerializedName("data")
        public MessageData data;

        public static class MessageData {
            @SerializedName("userMessage")
            public String userMessage;

            @SerializedName("aiResponse")
            public String aiResponse;
        }
    }
}