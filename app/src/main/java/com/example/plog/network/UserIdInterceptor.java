package com.example.plog.network;

import android.content.Context;
import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** @deprecated AuthInterceptor로 대체됨 */
@Deprecated
public class UserIdInterceptor implements Interceptor {

    public UserIdInterceptor(Context context) {}

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        return chain.proceed(chain.request());
    }
}
