package com.example.plog.network;

import com.example.plog.api.model.BookmarkRequest;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("/api/recommend/bookmarks")
    Call<Void> addBookmark(@Body BookmarkRequest req);

    @DELETE("/api/recommend/bookmarks/{contentId}")
    Call<Void> removeBookmark(@Path("contentId") String contentId);

    @GET("/api/recommend/bookmarks/{contentId}/status")
    Call<java.util.Map<String, Boolean>> isBookmarked(@Path("contentId") String contentId);
}
