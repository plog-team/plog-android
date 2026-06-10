package com.example.plog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.plog.databinding.ActivityMainBinding;
import com.example.plog.network.AuthInterceptor;
import com.example.plog.ui.auth.LoginActivity;
import com.example.plog.network.ApiClient;
import com.example.plog.ui.exchange.NotificationFragment;
import com.example.plog.network.RetrofitClient;
import com.example.plog.util.SessionManager;
import com.example.plog.notification.DiaryReminderScheduler;
import com.example.plog.notification.GeofenceManager;
import com.example.plog.notification.sync.PhotoLocationSyncManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private GeofenceManager geofenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ApiClient.init(this);
        RetrofitClient.init(this);

        if (!new SessionManager(this).isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupNavigation();

        // 매일 22시 일기 작성 알림 등록
        DiaryReminderScheduler.scheduleDailyDiaryReminder(this);

        // 서버 사진 위치 → 로컬 DB 동기화 (재방문 알림용)
        PhotoLocationSyncManager.sync(this);

        // 재방문 위치 알림용 권한 요청 + Foreground Service 시작
        geofenceManager = new GeofenceManager(this);
        geofenceManager.requestLocationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        AuthInterceptor.setAuthFailedListener(this::redirectToLogin);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AuthInterceptor.setAuthFailedListener(null);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupNavigation() {

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

            handleNotificationIntent(getIntent());

            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.homeFragment) {
                    navController.popBackStack(R.id.homeFragment, false);
                    navController.navigate(R.id.homeFragment);
                    return true;
                }
                NavigationUI.onNavDestinationSelected(item, navController);
                return true;
            });

            binding.ivNotification.setOnClickListener(v -> {
                NotificationFragment dialog = new NotificationFragment();
                dialog.show(getSupportFragmentManager(), "notification");
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                boolean isDiaryScreen = destination.getId() == R.id.diaryEditFragment
                        || destination.getId() == R.id.diaryDetailFragment;
                boolean isBookmarkScreen = destination.getId() == R.id.bookmarkFragment;

                binding.topBar.setVisibility((isDiaryScreen || isBookmarkScreen) ? View.GONE : View.VISIBLE);
                binding.divider.setVisibility((isDiaryScreen || isBookmarkScreen) ? View.GONE : View.VISIBLE);
                binding.bottomNavigation.setVisibility(isDiaryScreen ? View.GONE : View.VISIBLE);

                if (destination.getId() == R.id.homeFragment) {
                    binding.tvTitle.setText("홈");
                } else if (destination.getId() == R.id.recommendFragment) {
                    binding.tvTitle.setText("추천");
                } else if (destination.getId() == R.id.localFragment) {
                    binding.tvTitle.setText("로컬");
                } else if (destination.getId() == R.id.searchFragment) {
                    binding.tvTitle.setText("검색");
                } else if (destination.getId() == R.id.myFragment) {
                    binding.tvTitle.setText("My");
                } else if (
                        destination.getId() == R.id.notMatchedFragment ||
                                destination.getId() == R.id.matchingFragment ||
                                destination.getId() == R.id.matchedFragment ||
                                destination.getId() == R.id.matchConfirmFragment
                ) {
                    binding.tvTitle.setText("교환일기");
                }
            });
        }
    }

    // 위치 권한 승인 결과에 따라 Foreground Service 시작 처리
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (geofenceManager != null) {
            geofenceManager.handlePermissionResult(requestCode, grantResults);
        }
    }

    public void showNotification(String title, String message) {
        NotificationFragment dialog = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("message", message);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "notification");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        android.app.NotificationManager manager =
                (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        manager.cancel(2002);

        int cancelNotificationId = intent.getIntExtra("cancelNotificationId", -1);
        if (cancelNotificationId != -1) {
            manager.cancel(cancelNotificationId);
        }

        boolean openWriteDiary = intent.getBooleanExtra("openWriteDiary", false);

        if (openWriteDiary && navController != null) {
            navController.navigate(R.id.diaryEditFragment);
            return;
        }

        String openDiaryDate = intent.getStringExtra("openDiaryDate");
        if (openDiaryDate == null || openDiaryDate.isEmpty()) return;

        ApiClient.getApiService().getDiaries(50)
                .enqueue(new retrofit2.Callback<com.example.plog.model.ApiResponse<java.util.List<com.example.plog.model.DiarySimpleResponse>>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<com.example.plog.model.ApiResponse<java.util.List<com.example.plog.model.DiarySimpleResponse>>> call,
                            retrofit2.Response<com.example.plog.model.ApiResponse<java.util.List<com.example.plog.model.DiarySimpleResponse>>> response
                    ) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().data == null
                                || navController == null) {
                            return;
                        }

                        for (com.example.plog.model.DiarySimpleResponse diary : response.body().data) {
                            if (openDiaryDate.equals(diary.date)) {
                                Bundle bundle = new Bundle();
                                bundle.putLong("diaryId", diary.diaryId);
                                navController.navigate(R.id.diaryDetailFragment, bundle);
                                return;
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<com.example.plog.model.ApiResponse<java.util.List<com.example.plog.model.DiarySimpleResponse>>> call,
                            Throwable t
                    ) {
                        // 실패 시 무시
                    }
                });
    }
}
