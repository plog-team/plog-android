package com.example.plog.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // 교환일기 버튼 클릭 (테스트를 위한 버튼임)
        binding.btnDiary.setOnClickListener(v -> {

            boolean isMatched = false;

            if (isMatched) {

                NavHostFragment.findNavController(this)
                        .navigate(R.id.matchedFragment);

            } else {

                NavHostFragment.findNavController(this)
                        .navigate(R.id.notMatchedFragment);

            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}