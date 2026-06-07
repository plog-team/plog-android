package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class PhotoAnalysisDto {

    @SerializedName("photoId")
    public Long photoId;

    @SerializedName("sha256")
    public String sha256;

    @SerializedName("exif")
    public ExifResult exif;

    @SerializedName("vision")
    public VisionResult vision;

    @SerializedName("cacheHit")
    public boolean cacheHit;

    @SerializedName("latencyMs")
    public long latencyMs;
}
