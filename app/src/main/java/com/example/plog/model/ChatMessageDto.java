package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class ChatMessageDto {

    @SerializedName("id")
    public Long id;

    @SerializedName("role")
    public String role;

    @SerializedName("content")
    public String content;

    @SerializedName("orderIdx")
    public Integer orderIdx;

    public ChatMessageDto() {}

    public ChatMessageDto(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
