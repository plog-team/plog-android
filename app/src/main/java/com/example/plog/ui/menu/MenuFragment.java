package com.example.plog.ui.menu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;
import com.example.plog.ui.auth.LoginActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuFragment extends Fragment {

    public MenuFragment() {
        super(R.layout.fragment_menu);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadProfileImage(view);

        // 계정 관리
        view.findViewById(R.id.menu_account)
                .setOnClickListener(v -> {
                    // TODO
                });

        // 작성한 일기
        view.findViewById(R.id.menu_diary)
                .setOnClickListener(v -> {
                    // TODO
                });

        // 북마크
        view.findViewById(R.id.menu_bookmark)
                .setOnClickListener(v -> {
                    // TODO
                });

        // 비밀글
        view.findViewById(R.id.menu_private)
                .setOnClickListener(v -> {
                    // TODO
                });

        // 알림
        view.findViewById(R.id.menu_notification)
                .setOnClickListener(v -> {
                    // TODO
                });

        // 설정
        view.findViewById(R.id.menu_settings)
                .setOnClickListener(v -> {
                    // TODO
                });

        // 로그아웃
        view.findViewById(R.id.tv_logout)
                .setOnClickListener(v -> showLogoutDialog());
    }

    private void loadProfileImage(android.view.View view) {

        SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", requireContext().MODE_PRIVATE);

        String imageUrl = prefs.getString("profile_image_url", "");
        String username = prefs.getString("username", "유저명");
        String following = prefs.getString("following_count", "0");
        String followers = prefs.getString("follower_count", "0");

        ((TextView) view.findViewById(R.id.tv_username))
                .setText(username);

        ((TextView) view.findViewById(R.id.tv_following))
                .setText("팔로잉 " + following);

        ((TextView) view.findViewById(R.id.tv_followers))
                .setText("팔로워 " + followers);

        if (!imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_user_circle)
                    .error(R.drawable.bg_user_circle)
                    .circleCrop()
                    .into((ImageView) view.findViewById(R.id.iv_profile));
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃",
                        (dialog, which) -> logoutFromServer())
                .setNegativeButton("취소", null)
                .show();
    }

    private void logoutFromServer() {

        SharedPreferences prefs =
                requireContext().getSharedPreferences(
                        "plog_prefs",
                        requireContext().MODE_PRIVATE
                );

        String token = prefs.getString("token", "");

        ApiService apiService = ApiClient.getApiService();

        apiService.logout("Bearer " + token)
                .enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(Call<Void> call,
                                           Response<Void> response) {
                        clearTokenAndGoLogin();
                    }

                    @Override
                    public void onFailure(Call<Void> call,
                                          Throwable t) {
                        clearTokenAndGoLogin();
                    }
                });
    }

    private void clearTokenAndGoLogin() {

        requireContext()
                .getSharedPreferences("auth", requireContext().MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        requireContext()
                .getSharedPreferences("plog_prefs", requireContext().MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent =
                new Intent(requireContext(), LoginActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        requireActivity().finish();
    }
}