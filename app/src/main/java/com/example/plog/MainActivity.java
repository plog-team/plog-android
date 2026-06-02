package com.example.plog;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.plog.databinding.ActivityMainBinding;
import com.example.plog.ui.exchange.NotificationFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
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

            // 하단바 홈 버튼 클릭 시 홈으로 이동
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

            // 종 아이콘 클릭 시 알림 다이얼로그
            binding.ivNotification.setOnClickListener(v -> {
                NotificationFragment dialog = new NotificationFragment();
                dialog.show(getSupportFragmentManager(), "notification");
            });

            navController.addOnDestinationChangedListener(
                    (controller, destination, arguments) -> {

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

    public void showNotification(String title, String message) {
        NotificationFragment dialog = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("message", message);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "notification");
    }
}