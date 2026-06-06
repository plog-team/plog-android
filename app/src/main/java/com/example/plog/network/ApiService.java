package com.example.plog.network;

import com.example.plog.api.model.BookmarkRequest;
import com.example.plog.network.auth.EmailRequest;
import com.example.plog.network.auth.LoginRequest;
import com.example.plog.network.auth.LoginResponse;
import com.example.plog.network.auth.RegisterRequest;
import com.example.plog.network.auth.VerifyRequest;
import com.example.plog.api.model.ClickLogRequest;
import com.example.plog.api.model.PreferenceResponse;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // 북마크
    @POST("/api/recommend/bookmarks")
    Call<Void> addBookmark(@Body BookmarkRequest req);

    @DELETE("/api/recommend/bookmarks/{contentId}")
    Call<Void> removeBookmark(@Path("contentId") String contentId);

    @GET("/api/recommend/bookmarks/{contentId}/status")
    Call<Map<String, Boolean>> isBookmarked(@Path("contentId") String contentId);

    // 로그인, 회원가입
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<Void> register(@Body RegisterRequest request);

    // 이메일 인증
    @POST("api/auth/email/send")
    Call<Void> sendEmailCode(@Body EmailRequest request);

    @POST("api/auth/email/verify")
    Call<Void> verifyEmailCode(@Body VerifyRequest request);

    // 로그아웃
    @POST("/api/auth/logout")
    Call<Void> logout(@Header("Authorization") String token);
    @POST("api/recommend/clicklog")
    Call<Void> saveClickLog(@Body ClickLogRequest req);

    @GET("api/recommend/preference")
    Call<PreferenceResponse> getPreference();
}