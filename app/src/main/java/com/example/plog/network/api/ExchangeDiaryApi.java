package com.example.plog.network.api;

import com.example.plog.network.dto.ExchangeDiaryRequest;
import com.example.plog.network.dto.ExchangeDiaryResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ExchangeDiaryApi {

    @POST("api/exchange/diaries")
    Call<ExchangeDiaryResponse> createDiary(@Body ExchangeDiaryRequest request);

    @PATCH("api/exchange/diaries/{diaryId}")
    Call<ExchangeDiaryResponse> updateDiary(@Path("diaryId") Long diaryId, @Body Map<String, String> body);

    @GET("api/exchange/diaries/session/{sessionId}")
    Call<List<ExchangeDiaryResponse>> getDiaries(@Path("sessionId") Long sessionId);

    @GET("api/exchange/diaries/{diaryId}")
    Call<ExchangeDiaryResponse> getDiary(@Path("diaryId") Long diaryId);
}