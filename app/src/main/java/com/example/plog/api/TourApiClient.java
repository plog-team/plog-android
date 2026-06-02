package com.example.plog.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TourApiClient {

    // 관광공사 API
    private static final String TOUR_BASE_URL =
            "https://apis.data.go.kr/B551011/KorService2/";
    public static final String TOUR_API_KEY =
            "0bf54343e288b4b60ab5cbd7f063ab23791ed410c8855990cba319a33bc3af18";

    private static final String SEOUL_BASE_URL =
            "http://openapi.seoul.go.kr:8088/";
    public static final String SEOUL_API_KEY =
            "43724f6f7277686439384b53584e4d";

    public static TourApiService getTourService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging).build();
        return new Retrofit.Builder()
                .baseUrl(TOUR_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TourApiService.class);
    }

    public static SeoulApiService getSeoulService() {
        return new Retrofit.Builder()
                .baseUrl(SEOUL_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SeoulApiService.class);
    }
}