package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class PreferenceUpdateRequest {

    @SerializedName("preferredCategories")
    public final String preferredCategories;

    @SerializedName("categoryScores")
    public final Map<String, Float> categoryScores;

    public PreferenceUpdateRequest(List<String> categories, Map<String, Float> scores) {
        this.preferredCategories = String.join(",", categories);
        this.categoryScores = scores;
    }
}
