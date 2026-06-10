package com.example.plog.api.model;

import com.google.gson.annotations.SerializedName;

public class BookmarkItem {

    @SerializedName("contentId")
    public String contentId;

    @SerializedName("title")
    public String title;

    @SerializedName("address")
    public String address;

    @SerializedName("imageUrl")
    public String imageUrl;

    @SerializedName("category")
    public String category;

    @SerializedName("contentTypeId")
    public String contentTypeId;
}