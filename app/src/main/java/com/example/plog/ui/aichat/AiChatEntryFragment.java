package com.example.plog.ui.aichat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.plog.R;
import com.example.plog.databinding.FragmentAiChatEntryBinding;

public class AiChatEntryFragment extends Fragment {

    private FragmentAiChatEntryBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatEntryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // X 버튼 - 뒤로가기
        binding.btnClose.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        // 대화 시작하기 버튼
        binding.btnStartChat.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_aiChatEntry_to_aiChatOption)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}