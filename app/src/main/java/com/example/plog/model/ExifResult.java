package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class ExifResult {

    @SerializedName("capturedAt")
    public String capturedAt;

    @SerializedName("latitude")
    public Double latitude;

    @SerializedName("longitude")
    public Double longitude;

    @SerializedName("cameraMake")
    public String cameraMake;

    @SerializedName("cameraModel")
    public String cameraModel;

    @SerializedName("isoSpeed")
    public Integer isoSpeed;

    @SerializedName("exposureTime")
    public String exposureTime;

    @SerializedName("pixelWidth")
    public Integer pixelWidth;

    @SerializedName("pixelHeight")
    public Integer pixelHeight;
}
