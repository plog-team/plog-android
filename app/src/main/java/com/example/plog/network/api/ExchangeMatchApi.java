package com.example.plog.network.api;

import com.example.plog.network.dto.ExchangeMatchRequest;
import com.example.plog.network.dto.ExchangeMatchResponse;
import com.example.plog.network.dto.ExchangeRoomResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ExchangeMatchApi {

    // 매칭 신청
    @POST("api/exchange/matches")
    Call<ExchangeMatchResponse> createMatch(@Body ExchangeMatchRequest request);

    // 매칭 상태 확인
    @GET("api/exchange/matches/{matchId}")
    Call<ExchangeMatchResponse> getMatch(@Path("matchId") Long matchId);

    // 매칭 수락
    @POST("api/exchange/matches/{matchId}/accept")
    Call<ExchangeRoomResponse> acceptMatch(@Path("matchId") Long matchId);

    // 대기 중인 매칭 목록
    @GET("api/exchange/matches/pending")
    Call<List<ExchangeMatchResponse>> getPendingMatches();

    // 매칭 거절
    @POST("api/exchange/matches/{matchId}/reject")
    Call<Void> rejectMatch(@Path("matchId") Long matchId);
}