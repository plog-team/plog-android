package com.example.plog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.plog.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import android.app.PendingIntent;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        createNotificationChannel(); // ⭐ 채널은 앱 시작 시 1번만
    }

    private void setupNavigation() {

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.navHostFragment);

        if (navHostFragment != null) {

            navController = navHostFragment.getNavController();

            NavigationUI.setupWithNavController(
                    binding.bottomNavigation,
                    navController
            );

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

                int id = destination.getId();

                if (id == R.id.homeFragment) {
                    binding.tvTitle.setText("홈");

                } else if (id == R.id.recommendFragment) {
                    binding.tvTitle.setText("추천");

                } else if (id == R.id.localFragment) {
                    binding.tvTitle.setText("로컬");

                } else if (id == R.id.searchFragment) {
                    binding.tvTitle.setText("검색");

                } else if (id == R.id.myFragment) {
                    binding.tvTitle.setText("My");

                } else if (
                        id == R.id.notMatchedFragment ||
                                id == R.id.matchingFragment ||
                                id == R.id.matchedFragment ||
                                id == R.id.matchConfirmFragment
                ) {
                    binding.tvTitle.setText("교환일기");
                }
            });
        }
    }

    // =========================
    // 🔥 실제 매칭 알림
    // =========================
    public void showMatchSuccessNotification() {

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(this, "match_channel")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("교환일기 매칭 완료")
                        .setContentText("상대방이 신청을 수락했어요!")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        manager.notify(1001, builder.build());
    }

    // =========================
    // 🔥 채널 생성 (필수)
    // =========================
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    "match_channel",
                    "Match Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            manager.createNotificationChannel(channel);
        }
    }
}