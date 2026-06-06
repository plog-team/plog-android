package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class DiaryLineCommentResponse {
    @SerializedName("commentId")
    public long commentId;

    @SerializedName("diaryId")
    public long diaryId;

    @SerializedName("userId")
    public long userId;

    @SerializedName("authorName")
    public String authorName;

    @SerializedName("lineIndex")
    public int lineIndex;

    @SerializedName("content")
    public String content;

    @SerializedName("createdAt")
    public String createdAt;
}
