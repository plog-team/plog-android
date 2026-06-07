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

        ApiClient.init(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupNavigation();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

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

            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

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

    public void showNotification(String title, String message) {
        NotificationFragment dialog = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("message", message);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "notification");

    }
}