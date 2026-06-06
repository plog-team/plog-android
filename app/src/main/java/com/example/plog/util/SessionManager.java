package com.example.plog.util;

// util/SessionManager.java
import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME  = "diary_session";
    private static final String KEY_USER_ID = "user_id";
    private static final int    NO_USER     = -1;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 로그인 성공 시 팀원 코드에서 호출 */
    public void saveUserId(int userId) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    /** ViewModel에서 userId 꺼낼 때 사용 */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, NO_USER);
    }

    public boolean isLoggedIn() {
        return getUserId() != NO_USER;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
