package com.example.plog.network;

import com.example.plog.model.AnswerRequest;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.CreateSessionRequest;
import com.example.plog.model.CreateSessionResponse;
import com.example.plog.model.DraftResponse;
import com.example.plog.model.FeedbackRequest;
import com.example.plog.model.GuideQuestionDto;
import com.example.plog.model.PhotoUploadBatchResponse;
import com.example.plog.model.SendChatRequest;
import com.example.plog.model.SendChatResponse;
import com.example.plog.model.SessionDetailResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    /** 사진 멀티파트 업로드. files[] 배치 및 단일 file 파라미터 모두 허용. */
    @Multipart
    @POST("api/photos")
    Call<ApiResponse<PhotoUploadBatchResponse>> uploadPhoto(@Part MultipartBody.Part file);

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
    Call<ApiResponse<SendChatResponse>> sendChat(@Path("sessionId") long sessionId, @Body SendChatRequest request);

    @POST("api/ai-guide/sessions/{sessionId}/draft")
    Call<ApiResponse<DraftResponse>> generateDraft(@Path("sessionId") long sessionId);

    @POST("api/ai-guide/sessions/{sessionId}/confirm")
    Call<ApiResponse<SessionDetailResponse>> confirmSession(@Path("sessionId") long sessionId);

    @POST("api/ai-guide/sessions/{sessionId}/feedback")
    Call<Void> submitFeedback(@Path("sessionId") long sessionId, @Body FeedbackRequest request);
}
