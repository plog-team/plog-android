package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class DiaryLineCommentRequest {
    @SerializedName("lineIndex")
    public int lineIndex;

    @SerializedName("content")
    public String content;
}
