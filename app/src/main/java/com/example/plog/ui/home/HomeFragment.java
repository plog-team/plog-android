package com.example.plog.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.example.plog.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 뷰 바인딩 초기화
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 기존 일기 작성 배너 클릭
        binding.cardDiaryBanner.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_diaryEditFragment));

        // 2. 교환일기 배너 클릭 (상태에 따른 분기 처리)
        binding.cardExchangeBanner.setOnClickListener(v -> {
            SharedPreferences sharedPref = requireContext()
                    .getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE);

            // start_time이 0이면 매칭 기록이 없는 것(미매칭)
            long startTime = sharedPref.getLong("start_time", 0L);

            if (startTime != 0L) {
                // 매칭 중 -> 매칭 화면 이동
                NavHostFragment.findNavController(this).navigate(R.id.matchedFragment);
            } else {
                // 매칭 안 됨 -> 미매칭 화면 이동
                NavHostFragment.findNavController(this).navigate(R.id.notMatchedFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 메모리 누수 방지
    }
}