package com.example.plog.network.api;

import com.example.plog.network.dto.ExchangeDiaryRequest;
import com.example.plog.network.dto.ExchangeDiaryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ExchangeDiaryApi {

    // 일기 작성
    @POST("api/exchange/diaries")
    Call<ExchangeDiaryResponse> createDiary(@Body ExchangeDiaryRequest request);

    // 세션별 일기 목록 조회
    @GET("api/exchange/diaries/session/{sessionId}")
    Call<List<ExchangeDiaryResponse>> getDiaries(@Path("sessionId") Long sessionId);
}