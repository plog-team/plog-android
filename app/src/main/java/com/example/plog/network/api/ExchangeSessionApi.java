package com.example.plog.network.api;

import com.example.plog.network.dto.ExchangeSessionResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ExchangeSessionApi {

    @POST("api/exchange/sessions/{roomId}")
    Call<ExchangeSessionResponse> startSession(@Path("roomId") Long roomId);

    @GET("api/exchange/sessions/room/{roomId}")
    Call<ExchangeSessionResponse> getSessionByRoomId(@Path("roomId") Long roomId);
}