package com.example.plog.notification.api;

import com.example.plog.model.ApiResponse;
import com.example.plog.notification.dto.PhotoLocationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface PhotoLocationApi {

    @GET("/api/photos/locations")
    Call<ApiResponse<List<PhotoLocationResponse>>> getPhotoLocations();
}
