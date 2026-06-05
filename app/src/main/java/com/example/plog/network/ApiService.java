package com.example.plog.network;

import com.example.plog.api.model.BookmarkRequest;
import com.example.plog.model.AnswerRequest;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.ClarifyRequest;
import com.example.plog.model.CreateSessionRequest;
import com.example.plog.model.CreateSessionResponse;
import com.example.plog.model.DiarySimpleResponse;
import com.example.plog.model.DiaryEmojiDecorationRequest;
import com.example.plog.model.DiaryEmojiDecorationResponse;
import com.example.plog.model.DiaryLineCommentRequest;
import com.example.plog.model.DiaryLineCommentResponse;
import com.example.plog.model.DiaryLineCommentUpdateRequest;
import com.example.plog.model.DiaryUpsertRequest;
import com.example.plog.model.DraftResponse;
import com.example.plog.model.FeedbackRequest;
import com.example.plog.model.GenerateReportRequest;
import com.example.plog.model.GuideQuestionDto;
import com.example.plog.model.PhotoUploadBatchResponse;
import com.example.plog.model.PreferenceUpdateRequest;
import com.example.plog.model.ReportFeedbackRequest;
import com.example.plog.model.ReportStatusResponse;
import com.example.plog.model.SendChatRequest;
import com.example.plog.model.SendChatResponse;
import com.example.plog.model.SessionDetailResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;
import com.example.plog.model.PhotoAutoInputContext;

public interface ApiService {

    // ── 선호도 ──────────────────────────────────────────────────

    // TODO: 백엔드 컨트롤러 URL 확인 후 경로 수정 필요
    @PUT("api/preferences")
    Call<Void> updatePreferences(@Body PreferenceUpdateRequest request);

    // ── 북마크 ──────────────────────────────────────────────────

    @POST("/api/bookmarks")
    Call<Void> addBookmark(@Body BookmarkRequest req);

    @DELETE("/api/bookmarks/{contentId}")
    Call<Void> removeBookmark(@Path("contentId") String contentId);

    @GET("/api/bookmarks/{contentId}/status")
    Call<Map<String, Boolean>> isBookmarked(@Path("contentId") String contentId);

    // ── 사진 ────────────────────────────────────────────────────

    @Multipart
    @POST("api/photos")
    Call<ApiResponse<PhotoUploadBatchResponse>> uploadPhoto(@Part MultipartBody.Part file);

    @DELETE("api/photos/{photoId}")
    Call<Void> deletePhoto(@Path("photoId") long photoId);

    /** photoId로 사진 자동입력 정보를 조회 */
    @GET("api/photos/{photoId}/auto-input")
    Call<ApiResponse<PhotoAutoInputContext>> getPhotoAutoInput(
            @Path("photoId") long photoId
    );
    // ── AI 가이드 ────────────────────────────────────────────────

    @POST("api/ai-guide/sessions")
    Call<ApiResponse<CreateSessionResponse>> createAiSession(@Body CreateSessionRequest request);

    @GET("api/ai-guide/sessions/{sessionId}")
    Call<ApiResponse<SessionDetailResponse>> getAiSession(@Path("sessionId") long sessionId);

    @POST("api/ai-guide/sessions/{sessionId}/questions/{questionId}/answer")
    Call<ApiResponse<GuideQuestionDto>> answerQuestion(
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

    // ── 일기 열람 ──────────────────────────────────────────────

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

    // ── 장소 리포트 (월간) ──────────────────────────────────────

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

    // ── 감정 리포트 (주간) ──────────────────────────────────────

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
}
