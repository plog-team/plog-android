package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateSessionRequest {

    @SerializedName("photoIds")
    public List<Long> photoIds;

    @SerializedName("mode")
    public String mode;

    @SerializedName("persona")
    public String persona;

    public CreateSessionRequest(List<Long> photoIds) {
        this(photoIds, null, null);
    }

    public CreateSessionRequest(List<Long> photoIds, String mode) {
        this(photoIds, mode, null);
    }

    public CreateSessionRequest(List<Long> photoIds, String mode, String persona) {
        this.photoIds = photoIds;
        this.mode = mode;
        this.persona = persona;
    }
}
