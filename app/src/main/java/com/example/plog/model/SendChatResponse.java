package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class SendChatResponse {

    @SerializedName("assistantMessage")
    public String assistantMessage;

    @SerializedName("readyForDraft")
    public boolean readyForDraft;

    @SerializedName("userMessageId")
    public Long userMessageId;

    @SerializedName("assistantMessageId")
    public Long assistantMessageId;

    @SerializedName("latencyMs")
    public long latencyMs;
}
