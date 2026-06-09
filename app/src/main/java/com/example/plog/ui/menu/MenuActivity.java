package com.example.plog.ui.menu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;
import com.example.plog.ui.auth.LoginActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_menu);

        loadProfileImage();

        // 닫기 버튼
        findViewById(R.id.iv_close).setOnClickListener(v -> finish());

        // 계정 관리
        findViewById(R.id.menu_account).setOnClickListener(v -> {
            // TODO: startActivity(new Intent(this, AccountActivity.class));
        });

        // 작성한 일기
        findViewById(R.id.menu_diary).setOnClickListener(v -> {
            // TODO: startActivity(new Intent(this, MyDiaryActivity.class));
        });

        // 북마크
        findViewById(R.id.menu_bookmark).setOnClickListener(v -> {
            // TODO: startActivity(new Intent(this, BookmarkActivity.class));
        });

        // 비밀글
        findViewById(R.id.menu_private).setOnClickListener(v -> {
            // TODO: startActivity(new Intent(this, PrivatePostActivity.class));
        });

        // 알림
        findViewById(R.id.menu_notification).setOnClickListener(v -> {
            // TODO: startActivity(new Intent(this, NotificationActivity.class));
        });

        // 설정
        findViewById(R.id.menu_settings).setOnClickListener(v -> {
            // TODO: startActivity(new Intent(this, SettingsActivity.class));
        });

        // 로그아웃
        findViewById(R.id.tv_logout).setOnClickListener(v -> showLogoutDialog());
    }

    private void loadProfileImage() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String imageUrl = prefs.getString("profile_image_url", "");
        String username = prefs.getString("username", "유저명");
        String following = prefs.getString("following_count", "0");
        String followers = prefs.getString("follower_count", "0");

        // 유저 정보 세팅
        ((TextView) findViewById(R.id.tv_username)).setText(username);
        ((TextView) findViewById(R.id.tv_following)).setText("팔로잉 " + following);
        ((TextView) findViewById(R.id.tv_followers)).setText("팔로워 " + followers);

        // 프로필 이미지 로드
        if (!imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_user_circle)
                    .error(R.drawable.bg_user_circle)
                    .circleCrop()
                    .into((ImageView) findViewById(R.id.iv_profile));
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> logoutFromServer())
                .setNegativeButton("취소", null)
                .show();
    }

    private void logoutFromServer() {
        SharedPreferences prefs = getSharedPreferences("plog_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        ApiService apiService = ApiClient.getApiService();
        apiService.logout("Bearer " + token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                clearTokenAndGoLogin();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // 서버 통신 실패해도 로컬 토큰은 삭제
                clearTokenAndGoLogin();
            }
        });
    }

    private void clearTokenAndGoLogin() {
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit().clear().commit();

        getSharedPreferences("plog_prefs", MODE_PRIVATE)
                .edit().clear().commit();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
