package com.example.plog.network.api;

import com.example.plog.network.dto.MatchRecommendResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import com.example.plog.network.dto.MatchRecommendListResponse;

public interface MatchRecommendApi {

    @GET("api/exchange/recommend")
    Call<MatchRecommendListResponse> recommendMatches(@Query("userId") Long userId);
}