package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PreferenceUpdateRequest {

    @SerializedName("preferredCategories")
    public final String preferredCategories;

    public PreferenceUpdateRequest(List<String> categories) {
        this.preferredCategories = String.join(",", categories);
    }
}
