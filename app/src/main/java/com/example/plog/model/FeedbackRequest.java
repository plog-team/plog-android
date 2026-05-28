package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class FeedbackRequest {

    @SerializedName("satisfactionScore")
    public Integer satisfactionScore;

    @SerializedName("comment")
    public String comment;

    public FeedbackRequest(Integer satisfactionScore, String comment) {
        this.satisfactionScore = satisfactionScore;
        this.comment = comment;
    }
}
