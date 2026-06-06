package com.example.plog.api;

import com.example.plog.api.model.TourDetailResponse;
import com.example.plog.api.model.TourResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TourApiService {
    @GET("locationBasedList2")
    Call<TourResponse> getNearby(
            @Query("serviceKey")    String serviceKey,
            @Query("mapX")          double mapX,
            @Query("mapY")          double mapY,
            @Query("radius")        int radius,
            @Query("MobileApp")     String mobileApp,
            @Query("MobileOS")      String mobileOS,
            @Query("_type")         String type,
            @Query("numOfRows")     int numOfRows,
            @Query("pageNo")        int pageNo,
            @Query("contentTypeId") String contentTypeId
    );

    // 장소 상세 정보 API
    @GET("detailCommon2")
    Call<TourDetailResponse> getDetailWithType(
            @Query("serviceKey")  String serviceKey,
            @Query("contentId")   String contentId,
            @Query("MobileApp")   String mobileApp,
            @Query("MobileOS")    String mobileOS,
            @Query("_type")       String type
    );

    // 타입별 소개 정보 API (usetime, usefee, restdate 등)
    @GET("detailIntro2")
    Call<TourDetailResponse> getDetailIntro(
            @Query("serviceKey")    String serviceKey,
            @Query("contentId")     String contentId,
            @Query("contentTypeId") String contentTypeId,
            @Query("MobileApp")     String mobileApp,
            @Query("MobileOS")      String mobileOS,
            @Query("_type")         String type
    );
}
