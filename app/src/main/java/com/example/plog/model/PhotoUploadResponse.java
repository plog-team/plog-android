package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

public class PhotoUploadResponse {

    @SerializedName("photoId")
    public Long photoId;

    @SerializedName("sha256")
    public String sha256;

    @SerializedName("originalFilename")
    public String originalFilename;

    @SerializedName("mimeType")
    public String mimeType;

    @SerializedName("width")
    public Integer width;

    @SerializedName("height")
    public Integer height;

    @SerializedName("sizeBytes")
    public Long sizeBytes;

    @SerializedName("storedPath")
    public String storedPath;

    @SerializedName("cacheHit")
    public boolean cacheHit;
}
