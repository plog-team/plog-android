package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class DraftResponse {

    @SerializedName("sessionId")
    public Long sessionId;

    @SerializedName("draft")
    public String draft;

    @SerializedName("charCount")
    public Integer charCount;
}
