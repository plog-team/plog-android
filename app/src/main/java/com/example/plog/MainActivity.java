package com.example.plog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.plog.databinding.ActivityMainBinding;
import com.example.plog.ui.auth.LoginActivity;
import com.example.plog.ui.menu.MenuActivity;
import com.example.plog.network.ApiClient;
import com.example.plog.ui.exchange.NotificationFragment;

// 알림
import com.example.plog.util.SessionManager;
import com.example.plog.util.Constants;
import com.example.plog.notification.GeofenceManager;
import com.example.plog.notification.DiaryReminderScheduler;
import com.example.plog.notification.sync.PhotoLocationSyncManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private GeofenceManager geofenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ApiClient.init(this);

        /*
        SharedPreferences prefs = getSharedPreferences("plog_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        if (token.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

         */



        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupNavigation();


        // 일기 작성 알림 즉시 테스트 - 알림 되는지 바로 보고 싶으면 아래 코드
        // DiaryReminderScheduler.testDiaryReminderWorkerNow(this);

        // 22시에 실행됨
        DiaryReminderScheduler.scheduleDailyDiaryReminder(this);

        // userId: 1인 상태
        PhotoLocationSyncManager.sync(this, 1);

        /* 이후 변화 필요하다면 이렇게
        int rawUserId = new SessionManager(this).getUserId();

        long userId =
                rawUserId == -1
                        ? Constants.DEV_USER_ID
                        : rawUserId;

        PhotoLocationSyncManager.sync(this, userId);
         */

        // 재방문 위치 알림용 권한 요청 + Foreground Service 시작
        geofenceManager = new GeofenceManager(this);
        geofenceManager.requestLocationPermission();
    }

    private void setupNavigation() {

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.myFragment) {
                startActivity(new Intent(this, MenuActivity.class));
                return false; // nav_graph 이동 막기
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });

        // 탭 전환 시 상단 타이틀 변경
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
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
            }
        });

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
                binding.topBar.setVisibility(isDiaryScreen ? View.GONE : View.VISIBLE);
                binding.divider.setVisibility(isDiaryScreen ? View.GONE : View.VISIBLE);
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

    // // 위치 권한 승인 결과에 따라 Foreground Service 시작 처리
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
                        // 테스트용이므로 실패 시 무시
                    }
                });
    }

}

