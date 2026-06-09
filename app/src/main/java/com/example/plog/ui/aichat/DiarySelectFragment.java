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
import com.example.plog.databinding.FragmentDiarySelectBinding;

public class DiarySelectFragment extends Fragment {

    private FragmentDiarySelectBinding binding;
    private int selectedYear, selectedMonth, selectedDay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiarySelectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // X 버튼
        binding.btnClose.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        // 날짜 선택 시
        binding.calendarView.setOnDateChangeListener((calendarView, year, month, day) -> {
            Bundle args = new Bundle();
            args.putInt("year", year);
            args.putInt("month", month + 1);
            args.putInt("day", day);
            Navigation.findNavController(view)
                    .navigate(R.id.action_diarySelect_to_aiChat, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}