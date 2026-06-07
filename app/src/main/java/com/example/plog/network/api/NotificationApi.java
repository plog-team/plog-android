package com.example.plog.network.api;

import com.example.plog.network.dto.NotificationResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotificationApi {

    @GET("api/notifications")
    Call<List<NotificationResponse>> getNotifications(@Query("userId") Long userId);

    @GET("api/notifications/unread")
    Call<List<NotificationResponse>> getUnreadNotifications(@Query("userId") Long userId);

    @PATCH("api/notifications/{notificationId}/read")
    Call<Void> markAsRead(@Path("notificationId") Long notificationId);
}