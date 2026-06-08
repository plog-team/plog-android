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
import com.example.plog.databinding.FragmentAiChatOptionBinding;

public class AiChatOptionFragment extends Fragment {

    private FragmentAiChatOptionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatOptionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // X 버튼
        binding.btnClose.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        // 나의 하루 불러오기 → 달력 화면
        binding.btnLoadDiary.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_aiChatOption_to_diarySelect)
        );

        // 바로 대화 시작하기 → 채팅 화면
        binding.btnStartNow.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_aiChatOption_to_aiChat)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}