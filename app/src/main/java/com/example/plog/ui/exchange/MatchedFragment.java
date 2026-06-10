package com.example.plog.ui.exchange;

import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeDiaryApi;
import com.example.plog.network.api.ExchangeMatchApi;
import com.example.plog.network.api.ExchangeRoomApi;
import com.example.plog.network.api.ExchangeSessionApi;
import com.example.plog.network.api.ReportBlockApi;
import com.example.plog.network.dto.BlockRequest;
import com.example.plog.network.dto.ExchangeDiaryListResponse;
import com.example.plog.network.dto.ExchangeDiaryResponse;
import com.example.plog.network.dto.ExchangeMatchResponse;
import com.example.plog.network.dto.ExchangeRoomResponse;
import com.example.plog.network.dto.ExchangeSessionResponse;
import com.example.plog.network.dto.ReportRequest;
import com.example.plog.util.SessionManager;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchedFragment extends Fragment {

    private boolean isMine = true;
    private int currentDay = 1;
    private String partnerName = "사용자";
    private String sessionStartDate = null;
    private Long partnerUserId = null;
    private Long roomId = null;
    private Long sessionId = null;
    private List<ExchangeDiaryResponse.Data> diaryList = null;
    private ExchangeDiaryResponse.Data currentDiary = null;
    private TabLayout dayTab;

    private TextView tvUserName, tvDate, tvWeather, tvLocation, tvTitleDiary, tvBody;
    private CardView cvProfile;
    private MaterialButton btnEdit, btnWriteDiary;
    private View cvDiaryCard;
    private LinearLayout emptyDiaryLayout, diaryContentLayout;
    private CountDownTimer sessionTimer;
    private static final long SEVEN_DAYS_IN_MS = 7L * 24 * 60 * 60 * 1000;

    private final Handler extendPollingHandler = new Handler(Looper.getMainLooper());
    private Runnable extendPollingRunnable;
    private AlertDialog extendWaitingDialog;

    public MatchedFragment() {}

    private long getMyUserId() {
        return new SessionManager(requireContext()).getUserId();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_matched, container, false);

        Bundle args = getArguments();
        if (args != null) {
            partnerName = args.getString("partnerName", "사용자");
            long rid = args.getLong("roomId", -1L);
            if (rid != -1L) roomId = rid;
        }

        TabLayout typeTab = view.findViewById(R.id.typeTab);
        dayTab = view.findViewById(R.id.dayTab);
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

        setupDayTabs(7);

        cvProfile.setOnClickListener(v -> { if (!isMine) showReportPopup(v); });
        btnWriteDiary.setOnClickListener(v -> navigateToEdit());
        btnEdit.setOnClickListener(v -> navigateToEdit());

        typeTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                isMine = tab.getPosition() == 0;
                animateSmoothTransition(() -> displayDiary());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        dayTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentDay = tab.getPosition() + 1;
                animateSmoothTransition(() -> displayDiary());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (roomId != null) loadSession();

        return view;
    }

    private void setupDayTabs(int totalDays) {
        if (dayTab == null) return;
        dayTab.removeAllTabs();
        for (int i = 1; i <= totalDays; i++) {
            dayTab.addTab(dayTab.newTab().setText("Day" + i));
        }
    }

    private void loadSession() {
        ExchangeSessionApi sessionApi = RetrofitClient.getClient().create(ExchangeSessionApi.class);
        sessionApi.getSessionByRoomId(roomId).enqueue(new Callback<ExchangeSessionResponse>() {  // startSession → getSessionByRoomId
            @Override
            public void onResponse(Call<ExchangeSessionResponse> call, Response<ExchangeSessionResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    sessionId = response.body().getId();
                    updateSessionInfo(response.body());
                    loadDiaries();
                }
            }
            @Override
            public void onFailure(Call<ExchangeSessionResponse> call, Throwable t) {
                android.util.Log.e("MatchedFragment", "세션 조회 실패: " + t.getMessage());
            }
        });

        ExchangeRoomApi roomApi = RetrofitClient.getClient().create(ExchangeRoomApi.class);
        roomApi.getRoom(roomId).enqueue(new Callback<ExchangeRoomResponse>() {
            @Override
            public void onResponse(Call<ExchangeRoomResponse> call, Response<ExchangeRoomResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    loadPartnerInfo(response.body().getMatchId());
                }
            }
            @Override
            public void onFailure(Call<ExchangeRoomResponse> call, Throwable t) {
                android.util.Log.e("MatchedFragment", "교환방 조회 실패: " + t.getMessage());
            }
        });
    }

    private void updateSessionInfo(ExchangeSessionResponse session) {
        String startDate = session.getStartDate();
        String endDate = session.getEndDate();
        if (startDate == null || endDate == null) return;

        sessionStartDate = startDate;

        try {
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);

            long endTimeMs = end.atStartOfDay()
                    .toInstant(java.time.ZoneOffset.UTC)
                    .toEpochMilli();
            requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
                    .edit().putLong("start_time", endTimeMs - SEVEN_DAYS_IN_MS).apply();

            int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end);
            setupDayTabs(totalDays);

        } catch (Exception e) {
            android.util.Log.e("MatchedFragment", "날짜 파싱 오류: " + e.getMessage());
        }
    }

    private void reloadSessionAndValidate() {
        if (roomId == null) return;
        ExchangeSessionApi api = RetrofitClient.getClient().create(ExchangeSessionApi.class);
        api.getSessionByRoomId(roomId).enqueue(new Callback<ExchangeSessionResponse>() {
            @Override
            public void onResponse(Call<ExchangeSessionResponse> call, Response<ExchangeSessionResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    updateSessionInfo(response.body());
                    startSessionValidation();
                }
            }
            @Override
            public void onFailure(Call<ExchangeSessionResponse> call, Throwable t) {
                android.util.Log.e("MatchedFragment", "세션 재조회 실패: " + t.getMessage());
            }
        });
    }

    private void loadPartnerInfo(Long matchId) {
        ExchangeMatchApi matchApi = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        matchApi.getMatch(matchId, getMyUserId()).enqueue(new Callback<ExchangeMatchResponse>() {
            @Override
            public void onResponse(Call<ExchangeMatchResponse> call, Response<ExchangeMatchResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    String nickname = response.body().getRequesterNickname();
                    if (nickname != null && !nickname.isEmpty()) {
                        partnerName = nickname;
                        if (!isMine && tvUserName != null) tvUserName.setText(partnerName);
                    }
                    if (response.body().getPartnerUserId() != null) {
                        partnerUserId = response.body().getPartnerUserId();
                    }
                }
            }
            @Override
            public void onFailure(Call<ExchangeMatchResponse> call, Throwable t) {
                android.util.Log.e("MatchedFragment", "파트너 정보 조회 실패: " + t.getMessage());
            }
        });
    }

    private void loadDiaries() {
        if (sessionId == null) return;
        ExchangeDiaryApi api = RetrofitClient.getClient().create(ExchangeDiaryApi.class);
        api.getDiaries(sessionId).enqueue(new Callback<ExchangeDiaryListResponse>() {
            @Override
            public void onResponse(Call<ExchangeDiaryListResponse> call, Response<ExchangeDiaryListResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    diaryList = response.body().getData();
                    displayDiary();
                }
            }
            @Override
            public void onFailure(Call<ExchangeDiaryListResponse> call, Throwable t) {
                android.util.Log.e("MatchedFragment", "일기 목록 로드 실패: " + t.getMessage());
            }
        });
    }

    private void displayDiary() {
        if (diaryList == null) return;
        ExchangeDiaryResponse.Data found = null;
        for (ExchangeDiaryResponse.Data d : diaryList) {
            boolean isMyDiary = d.userId != null && d.userId.longValue() == getMyUserId();
            if (isMine == isMyDiary && d.dayNumber == currentDay) {
                found = d;
                break;
            }
        }
        currentDiary = found;
        updateUI(found);
    }

    private void updateUI(@Nullable ExchangeDiaryResponse.Data diary) {
        boolean isTabToday = (currentDay == getCurrentDayNumber());

        if (diary == null) {
            emptyDiaryLayout.setVisibility(View.VISIBLE);
            diaryContentLayout.setVisibility(View.GONE);
            btnWriteDiary.setVisibility(isMine && isTabToday ? View.VISIBLE : View.GONE);
            cvDiaryCard.setOnClickListener(null);
        } else {
            emptyDiaryLayout.setVisibility(View.GONE);
            diaryContentLayout.setVisibility(View.VISIBLE);
            tvDate.setText(diary.createdAt != null ? diary.createdAt.substring(0, 10) : "");
            tvWeather.setText("");
            tvLocation.setText("");
            tvTitleDiary.setText(diary.title != null ? diary.title : "");
            tvBody.setText(diary.content);

            cvDiaryCard.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putBoolean("isExchange", true);
                bundle.putLong("diaryId", diary.id);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_matchedFragment_to_diaryDetailFragment, bundle);
            });
        }

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) cvProfile.getLayoutParams();
        if (isMine) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            params.startToEnd = ConstraintLayout.LayoutParams.UNSET;
            params.endToStart = ConstraintLayout.LayoutParams.UNSET;
            tvUserName.setVisibility(View.GONE);
            btnEdit.setVisibility(diary != null && isTabToday ? View.VISIBLE : View.GONE);
        } else {
            params.startToStart = ConstraintLayout.LayoutParams.UNSET;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            params.startToEnd = ConstraintLayout.LayoutParams.UNSET;
            params.endToStart = ConstraintLayout.LayoutParams.UNSET;
            tvUserName.setVisibility(View.VISIBLE);
            tvUserName.setText(partnerName);
            btnEdit.setVisibility(View.GONE);
        }
        cvProfile.setLayoutParams(params);
        cvProfile.requestLayout();
    }

    private int getCurrentDayNumber() {
        if (sessionStartDate != null) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(sessionStartDate);
                java.time.LocalDate today = java.time.LocalDate.now();
                int day = (int) java.time.temporal.ChronoUnit.DAYS.between(start, today) + 1;
                return Math.max(1, Math.min(day, dayTab != null ? dayTab.getTabCount() : 7));
            } catch (Exception e) {
                android.util.Log.e("MatchedFragment", "날짜 계산 오류: " + e.getMessage());
            }
        }
        android.content.SharedPreferences sharedPref = requireActivity()
                .getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE);
        long startTime = sharedPref.getLong("start_time", System.currentTimeMillis());
        long diff = System.currentTimeMillis() - startTime;
        int day = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
        return Math.min(day, dayTab != null ? dayTab.getTabCount() : 7);
    }

    private void navigateToEdit() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("isExchange", true);
        bundle.putLong("sessionId", sessionId != null ? sessionId : 1L);
        bundle.putLong("userId", getMyUserId());
        bundle.putInt("dayNumber", currentDay);
        if (currentDiary != null) {
            bundle.putLong("diaryId", currentDiary.id);
        }
        Navigation.findNavController(requireView())
                .navigate(R.id.action_matchedFragment_to_exchangeDiaryEditFragment, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        startSessionValidation();
        View view = getView();
        if (view != null) {
            TabLayout typeTab = view.findViewById(R.id.typeTab);
            if (typeTab != null) {
                typeTab.selectTab(typeTab.getTabAt(isMine ? 0 : 1));
            }
        }
        loadDiaries();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sessionTimer != null) sessionTimer.cancel();
        stopExtendPolling();
        if (extendWaitingDialog != null && extendWaitingDialog.isShowing()) extendWaitingDialog.dismiss();
    }

    private void startSessionValidation() {
        Context context = requireActivity();
        android.content.SharedPreferences sharedPref = context.getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE);
        long startTime = sharedPref.getLong("start_time", 0L);
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
            sharedPref.edit().putLong("start_time", startTime).apply();
        }
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
                .setNegativeButton("교환 종료", (dialog, which) -> terminateSession())
                .show();
    }

    private void extendSession() {
        if (sessionId == null) return;
        ExchangeSessionApi api = RetrofitClient.getClient().create(ExchangeSessionApi.class);
        api.agreeExtend(sessionId, getMyUserId()).enqueue(new Callback<ExchangeSessionResponse>() {
            @Override
            public void onResponse(Call<ExchangeSessionResponse> call, Response<ExchangeSessionResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    extendWaitingDialog = new AlertDialog.Builder(requireContext())
                            .setTitle("연장 동의 완료")
                            .setMessage("상대방의 연장 동의를 기다리고 있어요.\n상대방이 동의하면 7일 연장됩니다.")
                            .setCancelable(false)
                            .setNegativeButton("동의 취소", (dialog, which) -> {
                                stopExtendPolling();
                                dialog.dismiss();
                                terminateSession();
                            })
                            .create();
                    extendWaitingDialog.show();
                    startExtendPolling();
                }
            }
            @Override
            public void onFailure(Call<ExchangeSessionResponse> call, Throwable t) {
                android.util.Log.e("MatchedFragment", "연장 동의 실패: " + t.getMessage());
            }
        });
    }

    private void startExtendPolling() {
        stopExtendPolling();
        extendPollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (sessionId == null || !isAdded()) return;
                ExchangeSessionApi api = RetrofitClient.getClient().create(ExchangeSessionApi.class);
                api.getExtendStatus(sessionId).enqueue(new Callback<Map<String, Boolean>>() {
                    @Override
                    public void onResponse(Call<Map<String, Boolean>> call, Response<Map<String, Boolean>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            Boolean allAgreed = response.body().get("allAgreed");
                            if (Boolean.TRUE.equals(allAgreed)) {
                                stopExtendPolling();
                                if (extendWaitingDialog != null && extendWaitingDialog.isShowing())
                                    extendWaitingDialog.dismiss();
                                Toast.makeText(requireContext(), "상호 동의! 7일 연장됐어요.", Toast.LENGTH_SHORT).show();
                                reloadSessionAndValidate();
                            } else {
                                extendPollingHandler.postDelayed(extendPollingRunnable, 3000);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Boolean>> call, Throwable t) {
                        if (isAdded()) extendPollingHandler.postDelayed(extendPollingRunnable, 3000);
                    }
                });
            }
        };
        extendPollingHandler.post(extendPollingRunnable);
    }

    private void stopExtendPolling() {
        if (extendPollingRunnable != null) {
            extendPollingHandler.removeCallbacks(extendPollingRunnable);
            extendPollingRunnable = null;
        }
    }

    private void terminateSession() {
        requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
                .edit().remove("start_time").apply();
        if (roomId != null) {
            ExchangeRoomApi roomApi = RetrofitClient.getClient().create(ExchangeRoomApi.class);
            roomApi.closeRoom(roomId).enqueue(new retrofit2.Callback<ExchangeRoomResponse>() {
                @Override
                public void onResponse(retrofit2.Call<ExchangeRoomResponse> call, retrofit2.Response<ExchangeRoomResponse> response) {
                    if (!isAdded()) return;
                    NavHostFragment.findNavController(MatchedFragment.this).navigate(R.id.notMatchedFragment);
                }
                @Override
                public void onFailure(retrofit2.Call<ExchangeRoomResponse> call, Throwable t) {
                    if (!isAdded()) return;
                    NavHostFragment.findNavController(MatchedFragment.this).navigate(R.id.notMatchedFragment);
                }
            });
        } else {
            NavHostFragment.findNavController(this).navigate(R.id.notMatchedFragment);
        }
    }

    private void animateSmoothTransition(Runnable onContentUpdate) {
        cvDiaryCard.animate().alpha(0.3f).scaleX(0.98f).scaleY(0.98f).translationY(10f)
                .setDuration(100).withEndAction(() -> {
                    onContentUpdate.run();
                    cvDiaryCard.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).translationY(0f)
                            .setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
                }).start();
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
        Long targetId = partnerUserId != null ? partnerUserId : 0L;
        ReportBlockApi api = RetrofitClient.getClient().create(ReportBlockApi.class);
        api.block(new BlockRequest(getMyUserId(), targetId)).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "차단되었습니다.", Toast.LENGTH_SHORT).show();
                closeRoomAndExit();
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                android.util.Log.e("Block", "차단 실패: " + t.getMessage());
                closeRoomAndExit();
            }
        });
    }

    private void showReasonDialog() {
        String[] reasons = {"부적절한 내용", "욕설/비방", "스팸"};
        new AlertDialog.Builder(requireContext())
                .setItems(reasons, (dialog, which) -> showFinalConfirmDialog(reasons[which]))
                .show();
    }

    private void showFinalConfirmDialog(String reason) {
        new AlertDialog.Builder(requireContext())
                .setTitle("신고하기")
                .setMessage("사유: " + reason)
                .setPositiveButton("계속", (dialog, which) -> showExitCompleteDialog(reason))
                .show();
    }

    private void showExitCompleteDialog(String reason) {
        Long targetId = partnerUserId != null ? partnerUserId : 0L;
        ReportBlockApi api = RetrofitClient.getClient().create(ReportBlockApi.class);
        api.report(new ReportRequest(getMyUserId(), targetId, reason)).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                closeRoomAndExit();
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                android.util.Log.e("Report", "신고 실패: " + t.getMessage());
                closeRoomAndExit();
            }
        });
    }

    private void closeRoomAndExit() {
        if (roomId == null) {
            exitToNotMatched();
            return;
        }
        ExchangeRoomApi roomApi = RetrofitClient.getClient().create(ExchangeRoomApi.class);
        roomApi.closeRoom(roomId).enqueue(new retrofit2.Callback<ExchangeRoomResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ExchangeRoomResponse> call, retrofit2.Response<ExchangeRoomResponse> response) {
                exitToNotMatched();
            }
            @Override
            public void onFailure(retrofit2.Call<ExchangeRoomResponse> call, Throwable t) {
                android.util.Log.e("Room", "교환방 종료 실패: " + t.getMessage());
                exitToNotMatched();
            }
        });
    }

    private void exitToNotMatched() {
        if (!isAdded()) return;
        requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
                .edit().remove("start_time").apply();
        NavHostFragment.findNavController(MatchedFragment.this).navigate(R.id.notMatchedFragment);
    }
}
