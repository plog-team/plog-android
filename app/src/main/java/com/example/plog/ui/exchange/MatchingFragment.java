package com.example.plog.ui.exchange;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;

public class MatchingFragment extends Fragment {

    public MatchingFragment() {
        super(R.layout.fragment_matching);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(view, savedInstanceState);

        view.postDelayed(() -> {

            NavHostFragment.findNavController(
                    MatchingFragment.this
            ).navigate(
                    R.id.matchConfirmFragment
            );

        }, 2000);
    }
}