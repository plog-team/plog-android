package com.example.plog.ui.exchange;

import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ReportBlockApi;
import com.example.plog.network.dto.BlockRequest;
import com.example.plog.network.dto.ReportRequest;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.example.plog.data.DiaryEntry;
import com.example.plog.data.DiaryRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MatchedFragment extends Fragment {

    private boolean isMine = true;
    private int currentDay = 1;
    private String partnerName = "사용자";
    private DiaryRepository repository;

    private TextView tvUserName, tvDate, tvWeather, tvLocation, tvTitleDiary, tvBody;
    private CardView cvProfile;
    private MaterialButton btnEdit, btnWriteDiary;
    private View cvDiaryCard;
    private LinearLayout emptyDiaryLayout, diaryContentLayout;
    private CountDownTimer sessionTimer;
    private static final long SEVEN_DAYS_IN_MS = 7L * 24 * 60 * 60 * 1000;

    public MatchedFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_matched, container, false);
        repository = new DiaryRepository(requireContext());

        Bundle args = getArguments();
        if (args != null) partnerName = args.getString("partnerName", "사용자");

        TabLayout typeTab = view.findViewById(R.id.typeTab);
        TabLayout dayTab = view.findViewById(R.id.dayTab);
        cvProfile = view.findViewById(R.id.cvProfile);
        tvDate = view.findViewById(R.id.tvDate);
        tvWeather = view.findViewById(R.id.tvWeather);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvTitleDiary = view.findViewById(R.id.tvTitleDiary);
        tvBody = view.findViewById(R.id.tvBody);
        tvUserName = view.findViewById(R.id.tvUserName);
        btnEdit = view.findViewById(R.id.btn_start_match);
        emptyDiaryLayout = view.findViewById(R.id.emptyDiaryLayout);
        diaryContentLayout = view.findViewById(R.id.diaryContentLayout);
        btnWriteDiary = view.findViewById(R.id.btnWriteDiary);
        cvDiaryCard = (View) tvTitleDiary.getParent();

        cvProfile.setOnClickListener(v -> { if (!isMine) showReportPopup(v); });
        btnWriteDiary.setOnClickListener(v -> navigateToEdit());
        btnEdit.setOnClickListener(v -> navigateToEdit());

        typeTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { isMine = tab.getPosition() == 0; animateSmoothTransition(() -> loadAndDisplayDiary()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {} @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        dayTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { currentDay = tab.getPosition() + 1; animateSmoothTransition(() -> loadAndDisplayDiary()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {} @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void navigateToEdit() {
        Bundle bundle = new Bundle();
        bundle.putLong("sessionId", 1L);
        bundle.putLong("userId", 1L);
        bundle.putInt("dayNumber", currentDay);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_matchedFragment_to_exchangeDiaryEditFragment, bundle);
    }

    private String getAuthorSpecificDateKey() {
        String prefix = isMine ? "my_" : "partner_";
        long startTime = requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE).getLong("start_time", System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        calendar.add(Calendar.DAY_OF_YEAR, currentDay - 1);
        return prefix + new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(calendar.getTime());
    }

    @Override
    public void onResume() {
        super.onResume();
        startSessionValidation();
        loadAndDisplayDiary();
    }

    private void loadAndDisplayDiary() {
        updateDiary(repository.getDiary(getAuthorSpecificDateKey()));
    }

    private void updateDiary(@Nullable DiaryEntry diary) {
        String realTodayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        String currentKey = getAuthorSpecificDateKey();
        boolean isTabToday = realTodayKey.equals(currentKey.substring(isMine ? 3 : 8));

        if (diary == null) {
            emptyDiaryLayout.setVisibility(View.VISIBLE);
            diaryContentLayout.setVisibility(View.GONE);
            btnWriteDiary.setVisibility(isMine && isTabToday ? View.VISIBLE : View.GONE);
        } else {
            emptyDiaryLayout.setVisibility(View.GONE);
            diaryContentLayout.setVisibility(View.VISIBLE);

            String rawDate = diary.getDate();
            String displayDate = rawDate.startsWith("my_") ? rawDate.substring(3)
                    : rawDate.startsWith("partner_") ? rawDate.substring(8) : rawDate;
            tvDate.setText(displayDate);

            tvWeather.setText(diary.getWeather());
            tvLocation.setText(diary.getLocation());
            tvTitleDiary.setText(diary.getTitle());
            tvBody.setText(diary.getBody());
        }

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) cvProfile.getLayoutParams();
        if (isMine) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            tvUserName.setVisibility(View.GONE);
            btnEdit.setVisibility(diary != null && isTabToday ? View.VISIBLE : View.GONE);
        } else {
            params.startToStart = ConstraintLayout.LayoutParams.UNSET;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            tvUserName.setVisibility(View.VISIBLE);
            tvUserName.setText(partnerName);
            btnEdit.setVisibility(View.GONE);
        }
        cvProfile.setLayoutParams(params);
    }

    @Override
    public void onPause() { super.onPause(); if (sessionTimer != null) sessionTimer.cancel(); }

    private void startSessionValidation() {
        Context context = requireActivity();
        android.content.SharedPreferences sharedPref = context.getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE);
        long startTime = sharedPref.getLong("start_time", 0L);
        if (startTime == 0L) { startTime = System.currentTimeMillis(); sharedPref.edit().putLong("start_time", startTime).apply(); }
        long remainingTime = (startTime + SEVEN_DAYS_IN_MS) - System.currentTimeMillis();
        if (remainingTime <= 0) showSessionEndDialog();
        else {
            if (sessionTimer != null) sessionTimer.cancel();
            sessionTimer = new CountDownTimer(remainingTime, 1000) {
                @Override public void onTick(long millisUntilFinished) {}
                @Override public void onFinish() { showSessionEndDialog(); }
            }.start();
        }
    }

    private void showSessionEndDialog() {
        if (!isAdded() || isRemoving()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("교환일기 기간 만료")
                .setMessage("약속된 7일간의 교환일기 기간이 종료되었습니다.")
                .setPositiveButton("기간 연장", (dialog, which) -> extendSession())
                .setNegativeButton("교환 종료", (dialog, which) -> terminateSession()).show();
    }

    private void extendSession() { requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE).edit().putLong("start_time", System.currentTimeMillis()).apply(); startSessionValidation(); }
    private void terminateSession() { requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE).edit().remove("start_time").apply(); NavHostFragment.findNavController(this).navigate(R.id.notMatchedFragment); }

    private void animateSmoothTransition(Runnable onContentUpdate) {
        cvDiaryCard.animate().alpha(0.3f).scaleX(0.98f).scaleY(0.98f).translationY(10f).setDuration(100).withEndAction(() -> { onContentUpdate.run(); cvDiaryCard.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).translationY(0f).setDuration(180).setInterpolator(new DecelerateInterpolator()).start(); }).start();
    }

    private void showReportPopup(View anchor) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_report, (ViewGroup) anchor.getParent(), false);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupView.findViewById(R.id.btnReport).setOnClickListener(v -> {
            popupWindow.dismiss();
            showReportDialog();
        });

        popupView.findViewById(R.id.btnBlock).setOnClickListener(v -> {
            popupWindow.dismiss();
            showBlockDialog();
        });

        popupWindow.showAsDropDown(anchor, 0, 10);
    }

    private void showReportDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("신고하기")
                .setMessage("이 사용자를 신고하시겠습니까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("계속", (dialog, which) -> showReasonDialog())
                .show();
    }

    private void showBlockDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("차단하기")
                .setMessage("이 사용자를 차단하시겠습니까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("차단", (dialog, which) -> blockUser())
                .show();
    }

    private void blockUser() {
        ReportBlockApi api = RetrofitClient.getClient().create(ReportBlockApi.class);
        api.block(new BlockRequest(1L, 2L)).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                android.util.Log.d("Block", "차단 완료");
                requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
                        .edit().remove("start_time").apply();
                NavHostFragment.findNavController(MatchedFragment.this).navigate(R.id.notMatchedFragment);
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                android.util.Log.e("Block", "차단 실패: " + t.getMessage());
            }
        });
    }

    private void showReasonDialog() { String[] reasons = {"부적절한 내용", "욕설/비방", "스팸"}; new AlertDialog.Builder(requireContext()).setItems(reasons, (dialog, which) -> showFinalConfirmDialog(reasons[which])).show(); }
    private void showFinalConfirmDialog(String reason) { new AlertDialog.Builder(requireContext()).setTitle("신고하기").setMessage("사유: " + reason).setPositiveButton("계속", (dialog, which) -> showExitCompleteDialog(reason)).show(); }

    private void showExitCompleteDialog(String reason) {
        ReportBlockApi api = RetrofitClient.getClient().create(ReportBlockApi.class);
        api.report(new ReportRequest(1L, 2L, reason)).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                android.util.Log.d("Report", "신고 완료");
                requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
                        .edit().remove("start_time").apply();
                NavHostFragment.findNavController(MatchedFragment.this).navigate(R.id.notMatchedFragment);
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                android.util.Log.e("Report", "신고 실패: " + t.getMessage());
            }
        });
    }
}