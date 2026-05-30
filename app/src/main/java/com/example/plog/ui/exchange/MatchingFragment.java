package com.example.plog.ui.exchange;

import android.os.Bundle;
import android.os.Handler;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 2초 후 상대방 찾아서 MatchConfirmFragment로 이동
        new Handler().postDelayed(() -> {
            if (getView() != null) {
                NavHostFragment.findNavController(MatchingFragment.this)
                        .navigate(R.id.matchConfirmFragment);
            }
        }, 2000);
    }
}