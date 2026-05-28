package com.example.plog.network;

import androidx.annotation.NonNull;

import com.example.plog.util.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** 모든 요청에 X-User-Id 헤더를 자동 부착. */
public class UserIdInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        Request authed = original.newBuilder()
                .header(Constants.HEADER_USER_ID, String.valueOf(Constants.TEMP_USER_ID))
                .build();
        return chain.proceed(authed);
    }
}
