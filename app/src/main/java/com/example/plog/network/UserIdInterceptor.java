package com.example.plog.network;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.example.plog.util.Constants;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserIdInterceptor implements Interceptor {

    private final Context context;

    public UserIdInterceptor(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        SharedPreferences prefs = context
                .getSharedPreferences("plog_prefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", (int) Constants.TEMP_USER_ID);
        Request request = chain.request().newBuilder()
                .header(Constants.HEADER_USER_ID, String.valueOf(userId))
                .build();

        return chain.proceed(request);
    }
}