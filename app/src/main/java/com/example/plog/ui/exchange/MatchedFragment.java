package com.example.plog.ui.exchange;

import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeDiaryApi;
import com.example.plog.network.api.ExchangeMatchApi;
import com.example.plog.network.api.ExchangeRoomApi;
import com.example.plog.network.api.ExchangeSessionApi;
import com.example.plog.network.api.ReportBlockApi;
import com.example.plog.network.dto.BlockRequest;
import com.example.plog.network.dto.ExchangeDiaryResponse;
import com.example.plog.network.dto.ExchangeMatchResponse;
import com.example.plog.network.dto.ExchangeRoomResponse;
import com.example.plog.network.dto.ExchangeSessionResponse;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchedFragment extends Fragment {

    private boolean isMine = true;
    private int currentDay = 1;
    private String partnerName = "사용자";
    private Long partnerUserId = null;
    private Long roomId = null;
    private Long sessionId = null;
    private List<ExchangeDiaryResponse> diaryList = null;
    private ExchangeDiaryResponse currentDiary = null;

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

        Bundle args = getArguments();
        if (args != null) {
            partnerName = args.getString("partnerName", "사용자");
            long rid = args.getLong("roomId", -1L);
            if (rid != -1L) roomId = rid;
        }

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

    private void loadSession() {
        ExchangeSessionApi sessionApi = RetrofitClient.getClient().create(ExchangeSessionApi.class);
        sessionApi.startSession(roomId).enqueue(new Callback<ExchangeSessionResponse>() {
            @Override
            public void onResponse(Call<ExchangeSessionResponse> call, Response<ExchangeSessionResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    sessionId = response.body().getId();
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
                    Long matchId = response.body().getMatchId();
                    loadPartnerInfo(matchId);
                }
            }
            @Override
            public void onFailure(Call<ExchangeRoomResponse> call, Throwable t) {
                android.util.Log.e("MatchedFragment", "교환방 조회 실패: " + t.getMessage());
            }
        });
    }

    private void loadPartnerInfo(Long matchId) {
        ExchangeMatchApi matchApi = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        matchApi.getMatch(matchId).enqueue(new Callback<ExchangeMatchResponse>() {
            @Override
            public void onResponse(Call<ExchangeMatchResponse> call, Response<ExchangeMatchResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    String nickname = response.body().getRequesterNickname();
                    if (nickname != null && !nickname.isEmpty()) {
                        partnerName = nickname;
                        if (!isMine && tvUserName != null) {
                            tvUserName.setText(partnerName);
                        }
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
        api.getDiaries(sessionId).enqueue(new Callback<List<ExchangeDiaryResponse>>() {
            @Override
            public void onResponse(Call<List<ExchangeDiaryResponse>> call, Response<List<ExchangeDiaryResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    diaryList = response.body();
                    displayDiary();
                }
            }
            @Override
            public void onFailure(Call<List<ExchangeDiaryResponse>> call, Throwable t) {
                android.util.Log.e("MatchedFragment", "일기 목록 로드 실패: " + t.getMessage());
            }
        });
    }

    private void displayDiary() {
        if (diaryList == null) return;
        ExchangeDiaryResponse found = null;
        for (ExchangeDiaryResponse d : diaryList) {
            boolean isMyDiary = d.getUserId() == 1L;
            if (isMine == isMyDiary && d.getDayNumber() == currentDay) {
                found = d;
                break;
            }
        }
        currentDiary = found;
        updateUI(found);
    }

    private void updateUI(@Nullable ExchangeDiaryResponse diary) {
        boolean isTabToday = (currentDay == getCurrentDayNumber());

        if (diary == null) {
            emptyDiaryLayout.setVisibility(View.VISIBLE);
            diaryContentLayout.setVisibility(View.GONE);
            btnWriteDiary.setVisibility(isMine && isTabToday ? View.VISIBLE : View.GONE);
            cvDiaryCard.setOnClickListener(null);
        } else {
            emptyDiaryLayout.setVisibility(View.GONE);
            diaryContentLayout.setVisibility(View.VISIBLE);
            tvDate.setText(diary.getCreatedAt() != null ? diary.getCreatedAt().substring(0, 10) : "");
            tvWeather.setText("");
            tvLocation.setText("");
            tvTitleDiary.setText("DAY " + diary.getDayNumber());
            tvBody.setText(diary.getContent());

            cvDiaryCard.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putLong("diaryId", diary.getId());
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
        android.content.SharedPreferences sharedPref = requireActivity()
                .getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE);
        long startTime = sharedPref.getLong("start_time", System.currentTimeMillis());
        long diff = System.currentTimeMillis() - startTime;
        int day = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
        return Math.min(day, 7);
    }

    private void navigateToEdit() {
        Bundle bundle = new Bundle();
        bundle.putLong("sessionId", sessionId != null ? sessionId : 1L);
        bundle.putLong("userId", 1L);
        bundle.putInt("dayNumber", currentDay);
        if (currentDiary != null) {
            bundle.putLong("diaryId", currentDiary.getId());
        }
        Navigation.findNavController(requireView())
                .navigate(R.id.action_matchedFragment_to_exchangeDiaryEditFragment, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        startSessionValidation();
        loadDiaries();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sessionTimer != null) sessionTimer.cancel();
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
        requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
                .edit().putLong("start_time", System.currentTimeMillis()).apply();
        startSessionValidation();
    }

    private void terminateSession() {
        requireActivity().getSharedPreferences("ExchangeSessionPref", Context.MODE_PRIVATE)
                .edit().remove("start_time").apply();
        NavHostFragment.findNavController(this).navigate(R.id.notMatchedFragment);
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
        api.block(new BlockRequest(1L, targetId)).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
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
        api.report(new ReportRequest(1L, targetId, reason)).enqueue(new retrofit2.Callback<Void>() {
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