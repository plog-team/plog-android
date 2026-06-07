package com.example.plog.ui.util;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.WeakHashMap;

/**
 * 텍스트를 한 글자씩 등장시키는 타이핑 효과.
 * 동시에 여러 TextView에 적용 가능. 같은 TextView에 새 효과 시작 시 이전 것은 자동 취소.
 */
public class TypingEffect {

    private static final WeakHashMap<TextView, Runnable> ACTIVE = new WeakHashMap<>();
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    /**
     * @param tv          대상 TextView
     * @param text        최종 텍스트
     * @param charDelayMs 글자 사이 간격 (15~40ms 추천)
     */
    public static void apply(TextView tv, String text, long charDelayMs) {
        apply(tv, text, charDelayMs, null);
    }

    public static void apply(TextView tv, String text, long charDelayMs, Runnable onComplete) {
        if (tv == null) return;
        cancel(tv);
        if (text == null || text.isEmpty()) {
            tv.setText("");
            if (onComplete != null) HANDLER.post(onComplete);
            return;
        }
        final String t = text;
        final int[] idx = {0};
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (tv.getWindowToken() == null) {
                    ACTIVE.remove(tv);
                    return;
                }
                idx[0]++;
                tv.setText(t.substring(0, Math.min(idx[0], t.length())));
                if (idx[0] < t.length()) {
                    HANDLER.postDelayed(this, charDelayMs);
                } else {
                    ACTIVE.remove(tv);
                    if (onComplete != null) HANDLER.post(onComplete);
                }
            }
        };
        ACTIVE.put(tv, task);
        tv.setText("");
        HANDLER.postDelayed(task, charDelayMs);
    }

    public static void cancel(TextView tv) {
        Runnable r = ACTIVE.remove(tv);
        if (r != null) HANDLER.removeCallbacks(r);
    }
}
