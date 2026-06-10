package com.example.plog.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.example.plog.util.SessionManager;
import java.io.IOException;
import java.lang.ref.WeakReference;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AuthInterceptor implements Interceptor {

    public interface AuthFailedListener {
        void onAuthFailed();
    }

    private static WeakReference<AuthFailedListener> listenerRef;

    public static void setAuthFailedListener(AuthFailedListener listener) {
        listenerRef = listener != null ? new WeakReference<>(listener) : null;
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();

        // 인증 API는 토큰 없이 호출
        if (path.startsWith("/api/auth/")) {
            return chain.proceed(original);
        }

        SharedPreferences prefs = context.getSharedPreferences("plog_prefs", Context.MODE_PRIVATE);
        SessionManager sessionManager = new SessionManager(context);
        String token = sessionManager.getToken();
        long userId = sessionManager.getUserId();

        Request.Builder requestBuilder = original.newBuilder();
        if (token != null && !token.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }
        if (userId > 0) {
            requestBuilder.header("X-User-Id", String.valueOf(userId));
        }
        Request request = requestBuilder.build();

        Response response = chain.proceed(request);

        if (response.code() == 400 || response.code() == 401) {
            ResponseBody body = response.peekBody(512);
            String bodyString = body != null ? body.string() : "";
            if (bodyString.contains("Authorization 헤더가 필요합니다")
                    || bodyString.contains("유효하지 않은 토큰입니다")) {
                prefs.edit().clear().apply();
                notifyAuthFailed();
            }
        }

        return response;
    }

    private void notifyAuthFailed() {
        if (listenerRef == null) return;
        AuthFailedListener listener = listenerRef.get();
        if (listener == null) return;
        mainHandler.post(listener::onAuthFailed);
    }
}
