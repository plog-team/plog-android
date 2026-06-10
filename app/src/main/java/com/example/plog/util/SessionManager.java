package com.example.plog.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME    = "plog_prefs";
    private static final String KEY_USER_ID  = "userId";
    private static final String KEY_TOKEN    = "token";
    private static final String KEY_LOGGED_IN = "isLoggedIn";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(long userId, String token) {
        prefs.edit()
                .remove(KEY_USER_ID)   // 구 int 타입 값 제거 후 재저장
                .putBoolean(KEY_LOGGED_IN, true)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_TOKEN, token)
                .apply();
    }

    /** @deprecated saveSession() 사용 권장 */
    @Deprecated
    public void saveUserId(int userId) {
        prefs.edit().remove(KEY_USER_ID).putLong(KEY_USER_ID, userId).apply();
    }

    public long getUserId() {
        try {
            return prefs.getLong(KEY_USER_ID, -1L);
        } catch (ClassCastException e) {
            // 구 버전에서 int로 저장된 경우
            int legacy = prefs.getInt(KEY_USER_ID, -1);
            prefs.edit().remove(KEY_USER_ID).putLong(KEY_USER_ID, legacy).apply();
            return legacy;
        }
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
