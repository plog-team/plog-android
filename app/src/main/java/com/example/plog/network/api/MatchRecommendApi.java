package com.example.plog.network.api;

import com.example.plog.network.dto.MatchRecommendResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MatchRecommendApi {

    @GET("api/exchange/recommend")
    Call<List<MatchRecommendResponse>> recommendMatches(@Query("userId") Long userId);
}