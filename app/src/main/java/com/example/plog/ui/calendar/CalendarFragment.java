package com.example.plog.ui.calendar;

import com.example.plog.R;
import com.example.plog.data.DiaryEntry;
import com.example.plog.data.DiaryRepository;

import android.net.Uri;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.FrameLayout;
import java.util.Calendar;

import java.text.SimpleDateFormat;
import java.util.Locale;
import android.widget.LinearLayout;
import android.widget.ImageView;

public class CalendarFragment extends Fragment {

    private DiaryRepository diaryRepository;
    private TextView tvMonthTitle;
    private GridLayout calendarGrid;
    private Calendar currentCalendar = Calendar.getInstance();
    private int selectedDay = -1;
    private int selectedMonth = -1;
    private int selectedYear = -1;
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    private LinearLayout recentDiaryContainer;

    public CalendarFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        diaryRepository = new DiaryRepository(requireContext());

        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        calendarGrid = view.findViewById(R.id.calendarGrid);

        // tvMonthTitle.setText("May 2026");

        TextView btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        TextView btnNextMonth = view.findViewById(R.id.btnNextMonth);

        recentDiaryContainer = view.findViewById(R.id.recentDiaryContainer);

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            generateCalendarDays();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            generateCalendarDays();
        });

        generateCalendarDays();
        generateRecentDiaries();

        return view;
    }

    private void generateCalendarDays() {
        calendarGrid.removeAllViews();

        Calendar calendar = (Calendar) currentCalendar.clone();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        tvMonthTitle.setText(new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.ENGLISH)
                .format(calendar.getTime()));

        calendar.set(year, month, 1);

        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int startBlankCount = firstDayOfWeek - Calendar.SUNDAY;

        if (startBlankCount < 0) {
            startBlankCount = 6;
        }

        // 앞쪽 빈칸
        for (int i = 0; i < startBlankCount; i++) {
            View blankView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_calendar_day, calendarGrid, false);

            TextView tvDay = blankView.findViewById(R.id.tvDay);
            tvDay.setText("");

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            blankView.setLayoutParams(params);

            calendarGrid.addView(blankView);
        }

        // 이번 달 날짜
        for (int day = 1; day <= daysInMonth; day++) {
            View dayView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_calendar_day, calendarGrid, false);

            TextView tvDay = dayView.findViewById(R.id.tvDay);
            tvDay.setText(String.valueOf(day));

            android.widget.ImageView imgDiary = dayView.findViewById(R.id.imgDiary);

            String dateKey = String.format(
                    Locale.KOREA,
                    "%04d-%02d-%02d",
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1,
                    day
            );

            DiaryEntry diary = diaryRepository.getDiary(dateKey);

            if (diary != null
                    && diary.getPhotoUris() != null
                    && !diary.getPhotoUris().isEmpty()) {

                imgDiary.setImageURI(Uri.parse(diary.getPhotoUris().get(0)));
                imgDiary.setVisibility(View.VISIBLE);

            } else {
                imgDiary.setVisibility(View.GONE);
            }

            View selectedBackground = dayView.findViewById(R.id.selectedBackground);

            if (day == selectedDay
                    && currentCalendar.get(Calendar.MONTH) == selectedMonth
                    && currentCalendar.get(Calendar.YEAR) == selectedYear) {
                selectedBackground.setVisibility(View.VISIBLE);
                tvDay.setTextColor(android.graphics.Color.WHITE);
            } else {
                selectedBackground.setVisibility(View.GONE);
                tvDay.setTextColor(android.graphics.Color.BLACK);
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dpToPx(48);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            dayView.setLayoutParams(params);

            int finalDay = day;

            dayView.setOnClickListener(v -> {
                selectedDay = finalDay;
                selectedMonth = currentCalendar.get(Calendar.MONTH);
                selectedYear = currentCalendar.get(Calendar.YEAR);

                generateCalendarDays();
            });

            calendarGrid.addView(dayView);
        }
    }

    private void generateRecentDiaries() {
        recentDiaryContainer.removeAllViews();

        for (int i = 3; i >= 1; i--) {
            View diaryView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_recent_diary, recentDiaryContainer, false);

            ImageView imgRecentDiary = diaryView.findViewById(R.id.imgRecentDiary);
            TextView tvRecentTitle = diaryView.findViewById(R.id.tvRecentTitle);
            TextView tvRecentDate = diaryView.findViewById(R.id.tvRecentDate);
            TextView tvRecentContent = diaryView.findViewById(R.id.tvRecentContent);

            // TODO: 서버/DB 연결 후 DiaryRepository에서 최근 일기 3개를 가져와 표시하기
            // 현재는 화면 테스트용 더미 데이터
            imgRecentDiary.setImageResource(R.drawable.test_photo);
            tvRecentTitle.setText("제목: 테스트 일기 " + i);
            tvRecentDate.setText("26/06/" + (10 + i));
            tvRecentContent.setText("내용내용내용 안보이는 부분은 말줄임표로 표시됩니다.");

            recentDiaryContainer.addView(diaryView);
        }
    }

}