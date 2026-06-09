package com.example.plog.ui.calendar;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.plog.R;
import com.example.plog.data.DiaryEntry;
import com.example.plog.data.DiaryRepository;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.DiarySimpleResponse;
import com.example.plog.network.ApiClient;
import com.example.plog.util.Constants;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.os.Bundle;
import androidx.navigation.Navigation;

public class CalendarFragment extends Fragment {

    private DiaryRepository diaryRepository;
    private TextView tvMonthTitle;
    private GridLayout calendarGrid;
    private Calendar currentCalendar = Calendar.getInstance();

    private int selectedDay = -1;
    private int selectedMonth = -1;
    private int selectedYear = -1;

    private LinearLayout recentDiaryContainer;
    private final Map<String, DiarySimpleResponse> serverDiaryMap = new HashMap<>();

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
        recentDiaryContainer = view.findViewById(R.id.recentDiaryContainer);

        TextView btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        TextView btnNextMonth = view.findViewById(R.id.btnNextMonth);

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            generateCalendarDays();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            generateCalendarDays();
        });

        generateCalendarDays();
        loadDiariesFromServer();

        return view;
    }

    private void generateCalendarDays() {
        calendarGrid.removeAllViews();

        Calendar calendar = (Calendar) currentCalendar.clone();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        tvMonthTitle.setText(
                new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.ENGLISH)
                        .format(calendar.getTime())
        );

        calendar.set(year, month, 1);

        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int startBlankCount = firstDayOfWeek - Calendar.SUNDAY;

        if (startBlankCount < 0) {
            startBlankCount = 6;
        }

        for (int i = 0; i < startBlankCount; i++) {
            View blankView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_calendar_day, calendarGrid, false);

            TextView tvDay = blankView.findViewById(R.id.tvDay);
            tvDay.setText("");

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dpToPx(48);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            blankView.setLayoutParams(params);

            calendarGrid.addView(blankView);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            View dayView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_calendar_day, calendarGrid, false);

            TextView tvDay = dayView.findViewById(R.id.tvDay);
            ImageView imgDiary = dayView.findViewById(R.id.imgDiary);
            View selectedBackground = dayView.findViewById(R.id.selectedBackground);

            tvDay.setText(String.valueOf(day));

            String dateKey = String.format(
                    Locale.KOREA,
                    "%04d-%02d-%02d",
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1,
                    day
            );

            DiarySimpleResponse diary = serverDiaryMap.get(dateKey);

            if (diary != null && diary.photoIds != null && !diary.photoIds.isEmpty()) {
                String photoUri = photoUrl(diary.photoIds.get(0));

                Glide.with(CalendarFragment.this)
                        .load(glidePhotoModel(photoUri))
                        .centerCrop()
                        .into(imgDiary);

                imgDiary.setVisibility(View.VISIBLE);
            } else {
                imgDiary.setImageDrawable(null);
                imgDiary.setVisibility(View.GONE);
            }

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
                DiarySimpleResponse clickedDiary = serverDiaryMap.get(dateKey);

                if (clickedDiary != null) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("diaryId", clickedDiary.diaryId);

                    Navigation.findNavController(v)
                            .navigate(R.id.diaryDetailFragment, bundle);
                    return;
                }

                selectedDay = finalDay;
                selectedMonth = currentCalendar.get(Calendar.MONTH);
                selectedYear = currentCalendar.get(Calendar.YEAR);

                generateCalendarDays();
            });

            calendarGrid.addView(dayView);
        }
    }


    private void loadDiariesFromServer() {
        recentDiaryContainer.removeAllViews();

        ApiClient.getApiService().getDiaries(50)
                .enqueue(new Callback<ApiResponse<List<DiarySimpleResponse>>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<List<DiarySimpleResponse>>> call,
                            @NonNull Response<ApiResponse<List<DiarySimpleResponse>>> response
                    ) {
                        if (!isAdded()) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().data != null) {

                            List<DiarySimpleResponse> diaries = response.body().data;

                            serverDiaryMap.clear();

                            for (DiarySimpleResponse diary : diaries) {
                                if (diary.date != null) {
                                    serverDiaryMap.put(diary.date, diary);
                                }
                            }

                            generateCalendarDays();
                            renderRecentDiaries(diaries);
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<List<DiarySimpleResponse>>> call,
                            @NonNull Throwable t
                    ) {
                        Log.e("CALENDAR", "일기 불러오기 실패", t);
                    }
                });
    }

    private String photoUrl(Long photoId) {
        String baseUrl = Constants.BASE_URL.endsWith("/")
                ? Constants.BASE_URL
                : Constants.BASE_URL + "/";

        return baseUrl + "api/photos/" + photoId;
    }

    private Object glidePhotoModel(String photoUri) {
        if (photoUri != null &&
                (photoUri.startsWith("http://") || photoUri.startsWith("https://"))) {

            return new GlideUrl(photoUri, new LazyHeaders.Builder()
                    .addHeader(Constants.HEADER_USER_ID, String.valueOf(Constants.DEV_USER_ID))
                    .build());
        }

        return Uri.parse(photoUri);
    }

    private String formatDateForRecent(String date) {
        if (date == null || date.length() != 10) {
            return "";
        }

        return date.substring(2, 4) + "/" +
                date.substring(5, 7) + "/" +
                date.substring(8, 10);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void renderRecentDiaries(List<DiarySimpleResponse> diaries) {
        recentDiaryContainer.removeAllViews();

        int count = Math.min(3, diaries.size());

        for (int i = 0; i < count; i++) {
            DiarySimpleResponse diary = diaries.get(i);

            View diaryView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_recent_diary, recentDiaryContainer, false);

            diaryView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putLong("diaryId", diary.diaryId);

                Navigation.findNavController(v)
                        .navigate(R.id.diaryDetailFragment, bundle);
            });

            ImageView imgRecentDiary = diaryView.findViewById(R.id.imgRecentDiary);
            TextView tvRecentTitle = diaryView.findViewById(R.id.tvRecentTitle);
            TextView tvRecentDate = diaryView.findViewById(R.id.tvRecentDate);
            TextView tvRecentContent = diaryView.findViewById(R.id.tvRecentContent);

            if (diary.photoIds != null && !diary.photoIds.isEmpty()) {
                String photoUri = photoUrl(diary.photoIds.get(0));

                Glide.with(CalendarFragment.this)
                        .load(glidePhotoModel(photoUri))
                        .centerCrop()
                        .into(imgRecentDiary);
            } else {
                imgRecentDiary.setImageResource(R.drawable.test_photo);
            }

            tvRecentTitle.setText(diary.title);
            tvRecentDate.setText(formatDateForRecent(diary.date));
            tvRecentContent.setText(diary.body);

            recentDiaryContainer.addView(diaryView);
        }
    }

}