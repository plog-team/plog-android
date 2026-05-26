package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class ReportFeedbackRequest {

    @SerializedName("rating")
    public int rating;

    @SerializedName("comment")
    public String comment;

    public ReportFeedbackRequest(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }
}
