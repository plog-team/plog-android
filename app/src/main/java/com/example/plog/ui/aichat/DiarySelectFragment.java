package com.example.plog.ui.aichat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.plog.R;
import com.example.plog.databinding.FragmentDiarySelectBinding;

import java.time.LocalDate;
import java.time.YearMonth;

public class DiarySelectFragment extends Fragment {

    private FragmentDiarySelectBinding binding;

    private int currentYear;
    private int currentMonth; // 1~12

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

        binding.btnClose.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        LocalDate now = LocalDate.now();
        currentYear = now.getYear();
        currentMonth = now.getMonthValue();

        drawCalendar(view);

        binding.btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;

            if (currentMonth == 0) {
                currentMonth = 12;
                currentYear--;
            }

            drawCalendar(view);
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            currentMonth++;

            if (currentMonth == 13) {
                currentMonth = 1;
                currentYear++;
            }

            drawCalendar(view);
        });
    }

    private void drawCalendar(View rootView) {

        binding.calendarGrid.removeAllViews();

        YearMonth yearMonth = YearMonth.of(currentYear, currentMonth);

        binding.tvMonthTitle.setText(
                currentYear + "년 " + currentMonth + "월"
        );

        int firstDay =
                yearMonth.atDay(1).getDayOfWeek().getValue() % 7;

        int days = yearMonth.lengthOfMonth();

        // 앞 공백
        for (int i = 0; i < firstDay; i++) {

            TextView empty = new TextView(requireContext());

            empty.setWidth(120);
            empty.setHeight(120);

            binding.calendarGrid.addView(empty);
        }

        // 날짜
        for (int day = 1; day <= days; day++) {

            final int selectedDay = day;

            TextView tv = new TextView(requireContext());

            tv.setText(String.valueOf(day));
            tv.setGravity(android.view.Gravity.CENTER);

            tv.setWidth(120);
            tv.setHeight(120);

            tv.setOnClickListener(v -> {

                Bundle args = new Bundle();

                args.putInt("year", currentYear);
                args.putInt("month", currentMonth);
                args.putInt("day", selectedDay);

                Navigation.findNavController(rootView)
                        .navigate(R.id.action_diarySelect_to_aiChat, args);
            });

            binding.calendarGrid.addView(tv);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}