package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class DiaryEmojiDecorationResponse {
    @SerializedName("decorationId")
    public long decorationId;

    @SerializedName("diaryId")
    public long diaryId;

    @SerializedName("userId")
    public long userId;

    @SerializedName("authorName")
    public String authorName;

    @SerializedName("emoji")
    public String emoji;

    @SerializedName("xRatio")
    public double xRatio;

    @SerializedName("yRatio")
    public double yRatio;

    @SerializedName("scale")
    public double scale;

    @SerializedName("rotation")
    public double rotation;

    @SerializedName("createdAt")
    public String createdAt;
}
