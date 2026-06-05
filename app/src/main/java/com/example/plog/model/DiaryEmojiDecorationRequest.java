package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class DiaryEmojiDecorationRequest {
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
}
