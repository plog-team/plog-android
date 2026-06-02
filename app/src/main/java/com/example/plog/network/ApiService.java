package com.example.plog.network;

import com.example.plog.network.aichat.AiChatMessageResponse;
import com.example.plog.network.aichat.AiChatSessionResponse;
import com.example.plog.network.auth.EmailRequest;
import com.example.plog.network.auth.LoginRequest;
import com.example.plog.network.auth.LoginResponse;
import com.example.plog.network.auth.RegisterRequest;
import com.example.plog.network.auth.VerifyRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // 날짜 없이 세션 시작 (FREE_CHAT)
    @POST("/api/chat/session")
    Call<AiChatSessionResponse> startSession(
            @Header("X-User-Id") Long userId,
            @Query("userId") Long userIdParam,
            @Query("type") String type
    );

    // 날짜 지정 세션 시작 (DIARY_ASSIST)
    @POST("/api/chat/session")
    Call<AiChatSessionResponse> startSessionWithDate(
            @Header("X-User-Id") Long userId,
            @Query("userId") Long userIdParam,
            @Query("type") String type,
            @Query("date") String date
    );

    // 메시지 전송
    @POST("/api/chat/session/{sessionId}/message")
    Call<AiChatMessageResponse> sendMessage(
            @Header("X-User-Id") Long userId,
            @Path("sessionId") Long sessionId,
            @Query("message") String message
    );

    // 로그인, 회원가입
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<Void> register(@Body RegisterRequest request);

    // 이메일 인증
    @POST("api/auth/email/send")
    Call<Void> sendEmailCode(
            @Body EmailRequest request
    );

    @POST("api/auth/email/verify")
    Call<Void> verifyEmailCode(
            @Body VerifyRequest request
    );

    // 로그아웃
    @POST("/api/auth/logout")
    Call<Void> logout(@Header("Authorization") String token);
}