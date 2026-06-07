package com.example.plog.network.api;

import com.example.plog.network.dto.ExchangeRoomResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ExchangeRoomApi {

    @GET("api/exchange/rooms/active")
    Call<ExchangeRoomResponse> getActiveRoom(@Query("userId") Long userId);

    @GET("api/exchange/rooms/{roomId}")
    Call<ExchangeRoomResponse> getRoom(@Path("roomId") Long roomId);

    @PATCH("api/exchange/rooms/{roomId}/close")
    Call<ExchangeRoomResponse> closeRoom(@Path("roomId") Long roomId);
}