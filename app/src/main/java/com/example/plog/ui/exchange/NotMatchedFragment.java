package com.example.plog.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.google.android.material.button.MaterialButton;

public class NotMatchedFragment extends Fragment {

    public NotMatchedFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_not_matched,
                container,
                false
        );

        // 매칭하기 버튼
        MaterialButton btnStartMatch =
                view.findViewById(
                        R.id.btn_start_match
                );

        // 버튼 클릭 시 이동
        btnStartMatch.setOnClickListener(v -> {

            NavHostFragment.findNavController(
                    NotMatchedFragment.this
            ).navigate(
                    R.id.matchingFragment
            );
        });

        return view;
    }
}