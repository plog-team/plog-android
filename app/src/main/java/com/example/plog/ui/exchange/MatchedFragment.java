package com.example.plog.ui.exchange;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

public class MatchedFragment extends Fragment {

    // 상태 관리 변수
    private boolean isMine = true;
    private int currentDay = 1;

    // UI 컴포넌트
    private TextView tvUserName;
    private CardView cvProfile;
    private TextView tvDate;
    private TextView tvWeather;
    private TextView tvLocation;
    private TextView tvTitleDiary;
    private TextView tvBody;
    private MaterialButton btnEdit;
    private View cvDiaryCard;

    // 세션 타이머
    private CountDownTimer sessionTimer;

    private static final long SEVEN_DAYS_IN_MS =
            7L * 24 * 60 * 60 * 1000;

    public MatchedFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_matched,
                container,
                false
        );

        final String partnerName;

        Bundle args = getArguments();

        if (args != null) {

            partnerName =
                    args.getString(
                            "partnerName",
                            "사용자"
                    );

        } else {

            partnerName = "사용자";
        }

        // 탭
        TabLayout typeTab =
                view.findViewById(R.id.typeTab);

        TabLayout dayTab =
                view.findViewById(R.id.dayTab);

        // 뷰 바인딩
        cvProfile =
                view.findViewById(R.id.cvProfile);

        tvDate =
                view.findViewById(R.id.tvDate);

        tvWeather =
                view.findViewById(R.id.tvWeather);

        tvLocation =
                view.findViewById(R.id.tvLocation);

        tvTitleDiary =
                view.findViewById(R.id.tvTitleDiary);

        tvBody =
                view.findViewById(R.id.tvBody);

        tvUserName =
                view.findViewById(R.id.tvUserName);

        btnEdit =
                view.findViewById(R.id.btn_start_match);

        cvDiaryCard = (View) tvTitleDiary.getParent();

        // 프로필 클릭
        cvProfile.setOnClickListener(v -> {

            if (!isMine) {
                showReportPopup(v);
            }
        });

        // 초기 데이터
        updateDiary(partnerName);

        // 작성자 탭
        typeTab.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(
                            TabLayout.Tab tab
                    ) {

                        isMine = tab.getPosition() == 0;

                        animateSmoothTransition(
                                () -> updateDiary(partnerName)
                        );
                    }

                    @Override
                    public void onTabUnselected(
                            TabLayout.Tab tab
                    ) {
                    }

                    @Override
                    public void onTabReselected(
                            TabLayout.Tab tab
                    ) {
                    }
                });

        // 날짜 탭
        dayTab.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(
                            TabLayout.Tab tab
                    ) {

                        currentDay =
                                tab.getPosition() + 1;

                        animateSmoothTransition(
                                () -> updateDiary(partnerName)
                        );
                    }

                    @Override
                    public void onTabUnselected(
                            TabLayout.Tab tab
                    ) {
                    }

                    @Override
                    public void onTabReselected(
                            TabLayout.Tab tab
                    ) {
                    }
                });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startSessionValidation();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (sessionTimer != null) {
            sessionTimer.cancel();
        }
    }

    // ==========================================
    // 세션 검증
    // ==========================================

    private void startSessionValidation() {

        Context context = requireActivity();

        android.content.SharedPreferences sharedPref =
                context.getSharedPreferences(
                        "ExchangeSessionPref",
                        Context.MODE_PRIVATE
                );

        long startTime =
                sharedPref.getLong(
                        "start_time",
                        0L
                );

        if (startTime == 0L) {

            startTime =
                    System.currentTimeMillis();

            sharedPref.edit()
                    .putLong(
                            "start_time",
                            startTime
                    )
                    .apply();
        }

        long currentTime =
                System.currentTimeMillis();

        long expireTime =
                startTime + SEVEN_DAYS_IN_MS;

        long remainingTime =
                expireTime - currentTime;

        if (remainingTime <= 0) {

            showSessionEndDialog();

        } else {

            if (sessionTimer != null) {
                sessionTimer.cancel();
            }

            sessionTimer =
                    new CountDownTimer(
                            remainingTime,
                            1000
                    ) {

                        @Override
                        public void onTick(
                                long millisUntilFinished
                        ) {
                        }

                        @Override
                        public void onFinish() {

                            showSessionEndDialog();
                        }
                    }.start();
        }
    }

    // ==========================================
    // 기간 만료
    // ==========================================

    private void showSessionEndDialog() {

        if (!isAdded() || isRemoving()) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("교환일기 기간 만료")
                .setMessage(
                        "약속된 7일간의 교환일기 기간이 종료되었습니다.\n\n"
                                + "이어서 일기를 계속 쓰시겠습니까?\n"
                                + "(나와 상대방이 모두 연장을 선택해야 기간이 7일 연장됩니다.)"
                )
                .setCancelable(false)
                .setPositiveButton(
                        "기간 연장",
                        (dialog, which) -> extendSession()
                )
                .setNegativeButton(
                        "교환 종료",
                        (dialog, which) -> terminateSession()
                )
                .show();
    }

    private void extendSession() {

        android.content.SharedPreferences sharedPref =
                requireActivity().getSharedPreferences(
                        "ExchangeSessionPref",
                        Context.MODE_PRIVATE
                );

        sharedPref.edit()
                .putLong(
                        "start_time",
                        System.currentTimeMillis()
                )
                .apply();

        Toast.makeText(
                requireContext(),
                "교환일기 기간이 7일 연장되었습니다.",
                Toast.LENGTH_SHORT
        ).show();

        startSessionValidation();
    }

    private void terminateSession() {

        android.content.SharedPreferences sharedPref =
                requireActivity().getSharedPreferences(
                        "ExchangeSessionPref",
                        Context.MODE_PRIVATE
                );

        sharedPref.edit()
                .remove("start_time")
                .apply();

        Toast.makeText(
                requireContext(),
                "교환일기가 완전히 종료되었습니다.",
                Toast.LENGTH_SHORT
        ).show();

        NavHostFragment.findNavController(this)
                .navigate(R.id.notMatchedFragment);
    }

    // ==========================================
    // 애니메이션
    // ==========================================

    private void animateSmoothTransition(
            Runnable onContentUpdate
    ) {

        cvDiaryCard.animate()
                .alpha(0.3f)
                .scaleX(0.98f)
                .scaleY(0.98f)
                .translationY(10f)
                .setDuration(100)
                .withEndAction(() -> {

                    onContentUpdate.run();

                    cvDiaryCard.animate()
                            .alpha(1.0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationY(0f)
                            .setDuration(180)
                            .setInterpolator(
                                    new DecelerateInterpolator()
                            )
                            .start();
                })
                .start();
    }

    // ==========================================
    // 일기 데이터 갱신
    // ==========================================

    private void updateDiary(
            String partnerName
    ) {

        ConstraintLayout.LayoutParams params =
                (ConstraintLayout.LayoutParams)
                        cvProfile.getLayoutParams();

        String writer =
                isMine ? "나" : partnerName;

        if (isMine) {

            params.startToStart =
                    ConstraintLayout.LayoutParams.PARENT_ID;

            params.endToEnd =
                    ConstraintLayout.LayoutParams.UNSET;

            tvUserName.setVisibility(View.GONE);

            btnEdit.setVisibility(View.VISIBLE);

        } else {

            params.startToStart =
                    ConstraintLayout.LayoutParams.UNSET;

            params.endToEnd =
                    ConstraintLayout.LayoutParams.PARENT_ID;

            tvUserName.setVisibility(View.VISIBLE);

            tvUserName.setText(partnerName);

            btnEdit.setVisibility(View.GONE);
        }

        cvProfile.setLayoutParams(params);

        tvDate.setText(
                "2026년 05월 "
                        + (20 + currentDay)
                        + "일"
        );

        if (currentDay % 2 == 0) {
            tvWeather.setText("☀️ 맑음");
        } else {
            tvWeather.setText("☁️ 흐림");
        }

        if (isMine) {
            tvLocation.setText("서울시 강남구");
        } else {
            tvLocation.setText("경기도 성남시");
        }

        tvTitleDiary.setText(
                currentDay
                        + "일차 - "
                        + writer
                        + "의 일기 제목"
        );

        tvBody.setText(
                "여기는 "
                        + currentDay
                        + "일차에 "
                        + writer
                        + "가 작성한 일기 내용이 들어가는 공간입니다."
        );
    }

    // ==========================================
    // 신고 팝업
    // ==========================================

    private void showReportPopup(View anchor) {

        View popupView =
                getLayoutInflater().inflate(
                        R.layout.popup_report,
                        (ViewGroup) anchor.getParent(),
                        false
                );

        PopupWindow popupWindow =
                new PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                );

        TextView btnReportBlock =
                popupView.findViewById(
                        R.id.btnReportBlock
                );

        btnReportBlock.setOnClickListener(v -> {

            popupWindow.dismiss();

            showExitConfirmDialog();
        });

        popupWindow.showAsDropDown(
                anchor,
                0,
                10
        );
    }

    private void showExitConfirmDialog() {

        new AlertDialog.Builder(requireContext())
                .setTitle("신고 및 차단")
                .setMessage(
                        "신고 및 차단 시 교환일기가 즉시 종료됩니다.\n계속하시겠습니까?"
                )
                .setNegativeButton(
                        "취소",
                        null
                )
                .setPositiveButton(
                        "계속",
                        (dialog, which) ->
                                showReasonDialog()
                )
                .show();
    }

    private void showReasonDialog() {

        String[] reasons = {
                "부적절한 내용",
                "욕설/비방",
                "스팸"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("신고 사유")
                .setItems(
                        reasons,
                        (dialog, which) ->
                                showFinalConfirmDialog(
                                        reasons[which]
                                )
                )
                .show();
    }

    private void showFinalConfirmDialog(
            String reason
    ) {

        new AlertDialog.Builder(requireContext())
                .setTitle("신고 및 차단")
                .setMessage(
                        "사유: "
                                + reason
                                + "\n\n계속 진행하시겠습니까?"
                )
                .setNegativeButton(
                        "취소",
                        null
                )
                .setPositiveButton(
                        "계속",
                        (dialog, which) ->
                                showExitCompleteDialog(reason)
                )
                .show();
    }

    private void showExitCompleteDialog(
            String reason
    ) {

        new AlertDialog.Builder(requireContext())
                .setTitle("종료")
                .setMessage(
                        "교환일기가 종료되었습니다.\n사유: "
                                + reason
                )
                .setPositiveButton(
                        "확인",
                        (dialog, which) -> {

                            android.content.SharedPreferences sharedPref =
                                    requireActivity()
                                            .getSharedPreferences(
                                                    "ExchangeSessionPref",
                                                    Context.MODE_PRIVATE
                                            );

                            sharedPref.edit()
                                    .remove("start_time")
                                    .apply();

                            NavHostFragment.findNavController(
                                    MatchedFragment.this
                            ).navigate(
                                    R.id.notMatchedFragment
                            );
                        }
                )
                .show();
    }
}