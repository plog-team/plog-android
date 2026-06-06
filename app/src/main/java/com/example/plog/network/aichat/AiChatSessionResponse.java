package com.example.plog.network.aichat;

import com.google.gson.annotations.SerializedName;

public class AiChatSessionResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public OuterData data;

    public static class OuterData {
        @SerializedName("data")
        public SessionData data;

        public static class SessionData {
            @SerializedName("sessionId")
            public long sessionId;

            @SerializedName("type")
            public String type;

            @SerializedName("aiResponse")
            public String aiResponse;
        }
    }
}