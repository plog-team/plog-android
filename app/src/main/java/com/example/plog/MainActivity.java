package com.example.plog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.plog.databinding.ActivityMainBinding;
import com.example.plog.ui.auth.LoginActivity;
import com.example.plog.ui.menu.MenuActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences prefs = getSharedPreferences("plog_prefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        if (token.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

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

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_my) {
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
    }
}