package com.example.plog.ui.util;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.WeakHashMap;

/**
 * AI 응답 대기 중 표시하는 typing dots 애니메이션.
 * "." → ".." → "..." → "...." 400ms 순환. TypingEffect와 독립적으로 동작.
 */
public class TypingDotsAnimator {

    private static final WeakHashMap<TextView, Runnable> ACTIVE = new WeakHashMap<>();
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final String[] FRAMES = {".", "..", "...", "...."};
    private static final long FRAME_MS = 400L;

    public static void start(TextView tv) {
        if (tv == null) return;
        stop(tv);
        final int[] frameIdx = {0};
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (tv.getWindowToken() == null) {
                    ACTIVE.remove(tv);
                    return;
                }
                tv.setText(FRAMES[frameIdx[0] % FRAMES.length]);
                frameIdx[0]++;
                HANDLER.postDelayed(this, FRAME_MS);
            }
        };
        ACTIVE.put(tv, task);
        HANDLER.post(task);
    }

    public static void stop(TextView tv) {
        Runnable r = ACTIVE.remove(tv);
        if (r != null) HANDLER.removeCallbacks(r);
    }
}
