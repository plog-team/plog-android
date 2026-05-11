package com.example.plog;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.plog.databinding.ActivityMainBinding;

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
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        navController = navHostFragment.getNavController();

        // BottomNav와 NavController 연결
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

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
    }
}