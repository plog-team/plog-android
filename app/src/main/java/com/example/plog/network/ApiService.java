package com.example.plog.network;

import com.example.plog.network.aichat.AiChatMessageResponse;
import com.example.plog.network.aichat.AiChatSessionDetailResponse;
import com.example.plog.network.aichat.AiChatSessionListResponse;
import com.example.plog.network.aichat.AiChatSessionResponse;
import com.example.plog.api.model.BookmarkRequest;
import com.example.plog.api.model.ClickLogRequest;
import com.example.plog.api.model.PreferenceResponse;
import com.example.plog.model.AnswerRequest;
import com.example.plog.model.AnswerResponse;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.ClarifyRequest;
import com.example.plog.model.CreateSessionRequest;
import com.example.plog.model.CreateSessionResponse;
import com.example.plog.model.DiaryEmojiDecorationRequest;
import com.example.plog.model.DiaryEmojiDecorationResponse;
import com.example.plog.model.DiaryLineCommentRequest;
import com.example.plog.model.DiaryLineCommentResponse;
import com.example.plog.model.DiaryLineCommentUpdateRequest;
import com.example.plog.model.DiarySimpleResponse;
import com.example.plog.model.DiaryUpsertRequest;
import com.example.plog.model.DraftResponse;
import com.example.plog.model.FeedbackRequest;
import com.example.plog.model.GenerateReportRequest;
import com.example.plog.model.GuideQuestionDto;
import com.example.plog.model.PhotoAutoInputContext;
import com.example.plog.model.PhotoUploadBatchResponse;
import com.example.plog.model.ReportFeedbackRequest;
import com.example.plog.model.ReportStatusResponse;
import com.example.plog.model.SendChatRequest;
import com.example.plog.model.SendChatResponse;
import com.example.plog.model.SessionDetailResponse;
import com.example.plog.network.auth.EmailRequest;
import com.example.plog.network.auth.LoginRequest;
import com.example.plog.network.auth.LoginResponse;
import com.example.plog.network.auth.RegisterRequest;
import com.example.plog.network.auth.VerifyRequest;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;
import com.example.plog.api.model.PlaceItemDto;
import com.example.plog.api.model.PlaceDetailDto;
import com.example.plog.api.model.CongestionDto;
import com.example.plog.model.PreferenceUpdateRequest;



public interface ApiService {

    // AI 챗봇 세션 시작 (FREE_CHAT)
    @POST("/api/chat/session")
    Call<AiChatSessionResponse> startSession(
            @Header("X-User-Id") Long userId,
            @Query("userId") Long userIdParam,
            @Query("type") String type
    );

    // AI 챗봇 세션 시작 (DIARY_ASSIST)
    @POST("/api/chat/session")
    Call<AiChatSessionResponse> startSessionWithDate(
            @Header("X-User-Id") Long userId,
            @Query("userId") Long userIdParam,
            @Query("type") String type,
            @Query("date") String date
    );

    // AI 챗봇 메시지 전송
    @POST("/api/chat/session/{sessionId}/message")
    Call<AiChatMessageResponse> sendMessage(
            @Header("X-User-Id") Long userId,
            @Path("sessionId") Long sessionId,
            @Query("message") String message
    );

    // 선호도
    @GET("api/recommend/preference")
    Call<PreferenceResponse> getPreference();
    @PUT("api/exchange/preferences")
    Call<Void> updatePreferences(@Header("X-User-Id") long userId,
                                 @Body PreferenceUpdateRequest req);

    // 북마크
    @POST("/api/recommend/bookmarks")
    Call<Void> addBookmark(@Body BookmarkRequest req);

    @DELETE("/api/recommend/bookmarks/{contentId}")
    Call<Void> removeBookmark(@Path("contentId") String contentId);

    @GET("/api/recommend/bookmarks/{contentId}/status")
    Call<Map<String, Boolean>> isBookmarked(@Path("contentId") String contentId);

    // 클릭로그
    @POST("api/recommend/clicklog")
    Call<Void> saveClickLog(@Body ClickLogRequest req);

    // 로그인 / 회원가입
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<Void> register(@Body RegisterRequest request);

    @POST("api/auth/email/send")
    Call<Void> sendEmailCode(@Body EmailRequest request);

    @POST("api/auth/email/verify")
    Call<Void> verifyEmailCode(@Body VerifyRequest request);

    @POST("/api/auth/logout")
    Call<Void> logout(@Header("Authorization") String token);

    // 사진

    @Multipart
    @POST("api/photos")
    Call<ApiResponse<PhotoUploadBatchResponse>> uploadPhoto(@Part MultipartBody.Part file);

    @DELETE("api/photos/{photoId}")
    Call<Void> deletePhoto(@Path("photoId") long photoId);

    @GET("api/photos/{photoId}/auto-input")
    Call<ApiResponse<PhotoAutoInputContext>> getPhotoAutoInput(@Path("photoId") long photoId);

    // AI 가이드

    @POST("api/ai-guide/sessions")
    Call<ApiResponse<CreateSessionResponse>> createAiSession(@Body CreateSessionRequest request);

    @GET("api/ai-guide/sessions/{sessionId}")
    Call<ApiResponse<SessionDetailResponse>> getAiSession(@Path("sessionId") long sessionId);

    @POST("api/ai-guide/sessions/{sessionId}/questions/{questionId}/answer")
    Call<ApiResponse<AnswerResponse>> answerQuestion(
        @Path("sessionId") long sessionId,
        @Path("questionId") long questionId,
        @Body AnswerRequest request
    );

    @POST("api/ai-guide/sessions/{sessionId}/chat")
    Call<ApiResponse<SendChatResponse>> sendChat(
        @Path("sessionId") long sessionId,
        @Body SendChatRequest request
    );

    @POST("api/ai-guide/sessions/{sessionId}/draft")
    Call<ApiResponse<DraftResponse>> generateDraft(@Path("sessionId") long sessionId);

    @POST("api/ai-guide/sessions/{sessionId}/confirm")
    Call<ApiResponse<SessionDetailResponse>> confirmSession(@Path("sessionId") long sessionId);

    @POST("api/ai-guide/sessions/{sessionId}/feedback")
    Call<Void> submitFeedback(
        @Path("sessionId") long sessionId,
        @Body FeedbackRequest request
    );

    // 일기
    @POST("api/diaries")
    Call<ApiResponse<DiarySimpleResponse>> saveDiary(@Body DiaryUpsertRequest request);

    @PUT("api/diaries/{diaryId}")
    Call<ApiResponse<DiarySimpleResponse>> updateDiary(
            @Path("diaryId") long diaryId,
            @Body DiaryUpsertRequest request
    );

    @GET("api/diaries/{diaryId}")
    Call<ApiResponse<DiarySimpleResponse>> getDiary(@Path("diaryId") long diaryId);

    @GET("api/diaries/by-date/{date}")
    Call<ApiResponse<DiarySimpleResponse>> getDiaryByDate(@Path("date") String date);

    @GET("api/diaries/{diaryId}/comments")
    Call<ApiResponse<List<DiaryLineCommentResponse>>> getDiaryComments(
            @Path("diaryId") long diaryId,
            @Query("lineIndex") Integer lineIndex
    );

    @POST("api/diaries/{diaryId}/comments")
    Call<ApiResponse<DiaryLineCommentResponse>> createDiaryComment(
            @Path("diaryId") long diaryId,
            @Body DiaryLineCommentRequest request
    );

    @PUT("api/diaries/{diaryId}/comments/{commentId}")
    Call<ApiResponse<DiaryLineCommentResponse>> updateDiaryComment(
            @Path("diaryId") long diaryId,
            @Path("commentId") long commentId,
            @Body DiaryLineCommentUpdateRequest request
    );

    @DELETE("api/diaries/{diaryId}/comments/{commentId}")
    Call<Void> deleteDiaryComment(
            @Path("diaryId") long diaryId,
            @Path("commentId") long commentId
    );

    @GET("api/diaries/{diaryId}/decorations")
    Call<ApiResponse<List<DiaryEmojiDecorationResponse>>> getDiaryDecorations(@Path("diaryId") long diaryId);

    @POST("api/diaries/{diaryId}/decorations")
    Call<ApiResponse<DiaryEmojiDecorationResponse>> createDiaryDecoration(
            @Path("diaryId") long diaryId,
            @Body DiaryEmojiDecorationRequest request
    );

    @PUT("api/diaries/{diaryId}/decorations/{decorationId}")
    Call<ApiResponse<DiaryEmojiDecorationResponse>> updateDiaryDecoration(
            @Path("diaryId") long diaryId,
            @Path("decorationId") long decorationId,
            @Body DiaryEmojiDecorationRequest request
    );

    @DELETE("api/diaries/{diaryId}/decorations/{decorationId}")
    Call<Void> deleteDiaryDecoration(
            @Path("diaryId") long diaryId,
            @Path("decorationId") long decorationId
    );

    // 장소 리포트
    @POST("api/report/place/generate")
    Call<ApiResponse<ReportStatusResponse>> generatePlaceReport(@Body GenerateReportRequest request);

    @GET("api/report/place/{threadId}")
    Call<ApiResponse<ReportStatusResponse>> getPlaceReportStatus(@Path("threadId") String threadId);

    @POST("api/report/place/{threadId}/clarify")
    Call<ApiResponse<ReportStatusResponse>> clarifyPlaceReport(
            @Path("threadId") String threadId,
            @Body ClarifyRequest request);

    @POST("api/report/place/{threadId}/feedback")
    Call<Void> submitPlaceReportFeedback(
            @Path("threadId") String threadId,
            @Body ReportFeedbackRequest request);

    // 감정 리포트
    @POST("api/report/emotion/generate")
    Call<ApiResponse<ReportStatusResponse>> generateEmotionReport(@Body GenerateReportRequest request);

    @GET("api/report/emotion/{threadId}")
    Call<ApiResponse<ReportStatusResponse>> getEmotionReportStatus(@Path("threadId") String threadId);

    @POST("api/report/emotion/{threadId}/clarify")
    Call<ApiResponse<ReportStatusResponse>> clarifyEmotionReport(
            @Path("threadId") String threadId,
            @Body ClarifyRequest request);

    @POST("api/report/emotion/{threadId}/feedback")
    Call<Void> submitEmotionReportFeedback(
            @Path("threadId") String threadId,
            @Body ReportFeedbackRequest request);

    // AI 챗봇 세션 목록 조회
    @GET("/api/chat/sessions")
    Call<AiChatSessionListResponse> getSessions(
            @Query("userId") Long userId
    );

    // AI 챗봇 세션 상세 조회 (이어하기)
    @GET("/api/chat/session/{sessionId}")
    Call<AiChatSessionDetailResponse> getSessionDetail(
            @Path("sessionId") Long sessionId
    );

    // AI 챗봇 세션 종료
    @DELETE("/api/chat/session/{sessionId}")
    Call<Void> endSession(
            @Path("sessionId") Long sessionId
    );

    // Tour API (백엔드 경유)
    @GET("api/tour/nearby")
    Call<ApiResponse<List<PlaceItemDto>>> getNearby(
            @Query("mapX") double mapX,
            @Query("mapY") double mapY,
            @Query("radius") int radius,
            @Query("numOfRows") int numOfRows,
            @Query("pageNo") int pageNo,
            @Query("contentTypeId") String contentTypeId);

    @GET("api/tour/detail")
    Call<ApiResponse<PlaceDetailDto>> getDetail(
            @Query("contentId") String contentId,
            @Query("contentTypeId") String contentTypeId);

    @GET("api/tour/congestion/{placeName}")
    Call<CongestionDto> getCongestion(@Path("placeName") String placeName);



}

