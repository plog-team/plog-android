package com.example.plog.network.api;

import com.example.plog.network.dto.BlockRequest;
import com.example.plog.network.dto.ReportRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ReportBlockApi {

    @POST("api/exchange/reports")
    Call<Void> report(@Body ReportRequest request);

    @POST("api/exchange/blocks")
    Call<Void> block(@Body BlockRequest request);
}