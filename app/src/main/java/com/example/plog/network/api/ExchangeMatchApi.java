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
import retrofit2.http.Query;

public interface ExchangeMatchApi {

    @POST("api/exchange/matches")
    Call<ExchangeMatchResponse> createMatch(@Body ExchangeMatchRequest request);

    @GET("api/exchange/matches/{matchId}")
    Call<ExchangeMatchResponse> getMatch(@Path("matchId") Long matchId, @Query("userId") Long userId);

    @POST("api/exchange/matches/{matchId}/accept")
    Call<ExchangeRoomResponse> acceptMatch(@Path("matchId") Long matchId);

    @GET("api/exchange/matches/pending")
    Call<List<ExchangeMatchResponse>> getPendingMatches(@Query("userId") Long userId);

    @POST("api/exchange/matches/{matchId}/reject")
    Call<Void> rejectMatch(@Path("matchId") Long matchId);

    @GET("api/exchange/matches/my-active")
    Call<ExchangeMatchResponse> getMyActiveMatch(@Query("userId") Long userId);
}