package com.example.plog;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.plog.databinding.ActivityMainBinding;
import com.example.plog.network.ApiClient;

// 테스트
import com.example.plog.notification.GeofenceManager;
import com.example.plog.notification.DiaryReminderScheduler;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private GeofenceManager geofenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApiClient.init(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();


        // 일기 작성 알림 즉시 테스트 - 알림 되는지 바로 보고 싶으면 아래 코드
        // DiaryReminderScheduler.testDiaryReminderWorkerNow(this);

        // 22시에 실행됨
        DiaryReminderScheduler.scheduleDailyDiaryReminder(this);

        // 재방문 위치 알림용 권한 요청 + Foreground Service 시작
        geofenceManager = new GeofenceManager(this);
        geofenceManager.requestLocationPermission();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        navController = navHostFragment.getNavController();

        // BottomNav와 NavController 연결
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        // 탭 전환 시 상단 타이틀 변경
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
            }
        });
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

}
