package com.example.plog.ui.report;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.plog.databinding.FragmentEmotionReportBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.ClarifyRequest;
import com.example.plog.model.DiarySimpleResponse;
import com.example.plog.model.EmotionByDay;
import com.example.plog.model.EmotionReportData;
import com.example.plog.model.GenerateReportRequest;
import com.example.plog.model.InterruptPayload;
import com.example.plog.model.ReportFeedbackRequest;
import com.example.plog.model.ReportStatusResponse;
import com.example.plog.network.ApiClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmotionReportFragment extends Fragment {

    private static final int STATE_LOADING = 0;
    private static final int STATE_INTERRUPTED = 1;
    private static final int STATE_DONE = 2;

    private static final int[] EMOTION_COLORS = {
            0xFF4CAF50, 0xFFFF9800, 0xFF2196F3,
            0xFFE91E63, 0xFF9C27B0, 0xFFF44336
    };

    // 별점 그라데이션: 1개(연보라) → 5개(#9C63F5, 버튼 primary 색과 동일)
    private static final int[] STAR_FILL_COLORS =
            {0xFFEDE0FC, 0xFFD4B8F9, 0xFFBF96F7, 0xFFAB7AF5, 0xFF9C63F5};
    private static final int STAR_EMPTY_COLOR = 0xFFBDBDBD;

    private FragmentEmotionReportBinding binding;
    private String threadId;
    private String userName = "사용자";
    private String emotionPeriod;
    private int selectedRating = 0;
    private TextView[] stars;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private Runnable typewriterRunnable;

    // ──────────────────────── Lifecycle ────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEmotionReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupEmotionPeriod();

        binding.btnClose.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());
        binding.headerEmotion.setOnClickListener(v ->
                animateAccordion(binding.contentEmotion, binding.tvEmotionChevron));
        binding.btnFeedback.setOnClickListener(v -> submitFeedback());
        setupStarRating();

        generateReport();
    }

    @Override
    public void onDestroyView() {
        stopPolling();
        if (typewriterRunnable != null) pollHandler.removeCallbacks(typewriterRunnable);
        binding = null;
        super.onDestroyView();
    }

    // ──────────────────────── Setup ────────────────────────

    private void setupEmotionPeriod() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("M월 d일", Locale.KOREAN);
        String endDisplay = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -6);
        String startDisplay = sdf.format(cal.getTime());
        emotionPeriod = startDisplay + " - " + endDisplay;
    }

    // ──────────────────────── Accordion animation ────────────────────────

    private void animateAccordion(View content, View chevron) {
        if (content.getVisibility() == View.GONE) {
            expandSection(content);
            chevron.animate()
                    .rotation(180f)
                    .setDuration(250)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            collapseSection(content);
            chevron.animate()
                    .rotation(0f)
                    .setDuration(200)
                    .setInterpolator(new AccelerateInterpolator())
                    .start();
        }
    }

    private void expandSection(View view) {
        view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int targetHeight = view.getMeasuredHeight();

        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);

        ValueAnimator anim = ValueAnimator.ofInt(0, targetHeight);
        anim.addUpdateListener(va -> {
            view.getLayoutParams().height = (int) va.getAnimatedValue();
            view.requestLayout();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        });
        anim.setDuration(300);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private void collapseSection(View view) {
        final int initialHeight = view.getMeasuredHeight();

        ValueAnimator anim = ValueAnimator.ofInt(initialHeight, 0);
        anim.addUpdateListener(va -> {
            view.getLayoutParams().height = (int) va.getAnimatedValue();
            view.requestLayout();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                view.setVisibility(View.GONE);
                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        });
        anim.setDuration(250);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.start();
    }

    // 아래서 올라오며 나타나는 뷰 등장 애니메이션
    private void animateIn(View view) {
        view.setAlpha(0f);
        view.setTranslationY(dpToPx(24));
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(380)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    // ──────────────────────── Star rating ────────────────────────

    private void setupStarRating() {
        stars = new TextView[]{binding.star1, binding.star2, binding.star3, binding.star4, binding.star5};
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateStarColors();
                // 탭 바운스 효과
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(90)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(90).start())
                        .start();
            });
        }
        updateStarColors();
    }

    private void updateStarColors() {
        if (stars == null) return;
        int fillColor = selectedRating > 0 ? STAR_FILL_COLORS[selectedRating - 1] : STAR_EMPTY_COLOR;
        for (int i = 0; i < stars.length; i++) {
            stars[i].setTextColor(i < selectedRating ? fillColor : STAR_EMPTY_COLOR);
        }
    }

    // ──────────────────────── Typewriter ────────────────────────

    // 타이핑 효과: 한 글자씩 서서히 표시
    private void typewriterEffect(TextView tv, String text) {
        if (typewriterRunnable != null) pollHandler.removeCallbacks(typewriterRunnable);
        tv.setText("");
        final int[] index = {0};
        typewriterRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || binding == null) return;
                if (index[0] <= text.length()) {
                    tv.setText(text.substring(0, index[0]));
                    index[0]++;
                    if (index[0] <= text.length()) pollHandler.postDelayed(this, 20);
                }
            }
        };
        pollHandler.post(typewriterRunnable);
    }

    // ──────────────────────── State ────────────────────────

    private void showState(int state) {
        binding.layoutLoading.setVisibility(state == STATE_LOADING ? View.VISIBLE : View.GONE);
        binding.layoutLoading.setAlpha(1f);
        binding.layoutInterrupt.setVisibility(state == STATE_INTERRUPTED ? View.VISIBLE : View.GONE);
        if (state == STATE_LOADING || state == STATE_INTERRUPTED) {
            binding.cardEmotion.setVisibility(View.GONE);
            binding.layoutFeedback.setVisibility(View.GONE);
        }
    }

    // ──────────────────────── Generate & Poll ────────────────────────

    private void generateReport() {
        showState(STATE_LOADING);

        ApiClient.getApiService().generateEmotionReport(new GenerateReportRequest())
                .enqueue(new Callback<ApiResponse<ReportStatusResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                           @NonNull Response<ApiResponse<ReportStatusResponse>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            threadId = response.body().data.threadId;
                            startPolling();
                        } else {
                            toast("감정 리포트 생성을 시작할 수 없어요");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        toast("네트워크 오류가 발생했어요");
                    }
                });
    }

    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || threadId == null) return;

                ApiClient.getApiService().getEmotionReportStatus(threadId)
                        .enqueue(new Callback<ApiResponse<ReportStatusResponse>>() {
                            @Override
                            public void onResponse(
                                    @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                    @NonNull Response<ApiResponse<ReportStatusResponse>> response) {
                                if (!isAdded()) return;
                                if (!response.isSuccessful() || response.body() == null
                                        || !response.body().success) {
                                    pollHandler.postDelayed(pollRunnable, 2000);
                                    return;
                                }
                                ReportStatusResponse data = response.body().data;
                                if (data.userName != null) userName = data.userName;
                                switch (data.status) {
                                    case "done":
                                        showResult(data);
                                        break;
                                    case "interrupted":
                                        stopPolling();
                                        showInterrupt(data.interruptPayload);
                                        break;
                                    case "error":
                                        toast(data.message != null
                                                ? data.message : "감정 분석 중 오류가 발생했어요");
                                        break;
                                    default:
                                        pollHandler.postDelayed(pollRunnable, 2000);
                                }
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                    @NonNull Throwable t) {
                                if (isAdded()) pollHandler.postDelayed(pollRunnable, 2000);
                            }
                        });
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void stopPolling() {
        if (pollRunnable != null) pollHandler.removeCallbacks(pollRunnable);
    }

    // ──────────────────────── Interrupt ────────────────────────

    private void showInterrupt(InterruptPayload payload) {
        binding.layoutLoading.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    if (!isAdded()) return;
                    binding.layoutLoading.setVisibility(View.GONE);
                    binding.layoutLoading.setAlpha(1f);

                    binding.tvInterruptDate.setText(payload.date + " 일기");
                    binding.tvInterruptQuestion.setText(payload.question);
                    binding.layoutDiaryContent.setVisibility(View.GONE);
                    binding.tvDiaryBody.setText("");
                    binding.progressInterrupt.setVisibility(View.GONE);

                    // 일기 열람 토글
                    final boolean[] diaryLoaded = {false};
                    binding.tvDiaryToggle.setOnClickListener(v -> {
                        if (binding.layoutDiaryContent.getVisibility() == View.VISIBLE) {
                            binding.layoutDiaryContent.setVisibility(View.GONE);
                            binding.tvDiaryToggle.setText("일기 열람 ▼");
                        } else if (diaryLoaded[0]) {
                            binding.layoutDiaryContent.setVisibility(View.VISIBLE);
                            binding.tvDiaryToggle.setText("일기 접기 ▲");
                        } else {
                            binding.tvDiaryToggle.setText("불러오는 중...");
                            ApiClient.getApiService().getDiary(payload.diaryId)
                                    .enqueue(new Callback<ApiResponse<DiarySimpleResponse>>() {
                                        @Override
                                        public void onResponse(@NonNull Call<ApiResponse<DiarySimpleResponse>> call,
                                                               @NonNull Response<ApiResponse<DiarySimpleResponse>> response) {
                                            if (!isAdded()) return;
                                            if (response.isSuccessful() && response.body() != null
                                                    && response.body().success) {
                                                DiarySimpleResponse diary = response.body().data;
                                                String bodyText = (diary.body != null && !diary.body.isBlank())
                                                        ? diary.body : "(내용 없음)";
                                                binding.tvDiaryBody.setText(bodyText);
                                                diaryLoaded[0] = true;
                                                binding.layoutDiaryContent.setVisibility(View.VISIBLE);
                                                binding.tvDiaryToggle.setText("일기 접기 ▲");
                                            } else {
                                                binding.tvDiaryToggle.setText("일기 열람 ▼");
                                                toast("일기를 불러올 수 없어요");
                                            }
                                        }

                                        @Override
                                        public void onFailure(@NonNull Call<ApiResponse<DiarySimpleResponse>> call,
                                                              @NonNull Throwable t) {
                                            if (!isAdded()) return;
                                            binding.tvDiaryToggle.setText("일기 열람 ▼");
                                            toast("일기 로딩에 실패했어요");
                                        }
                                    });
                        }
                    });

                    // 선택지 버튼 or 자유 입력
                    binding.layoutOptions.removeAllViews();
                    boolean hasOptions = payload.options != null && !payload.options.isEmpty();
                    if (hasOptions) {
                        binding.layoutOptions.setVisibility(View.VISIBLE);
                        binding.layoutFreeAnswer.setVisibility(View.GONE);
                        for (String option : payload.options) {
                            Button btn = new Button(requireContext());
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, 0, 0, dpToPx(8));
                            btn.setLayoutParams(params);
                            btn.setText(option);
                            btn.setAllCaps(false);
                            btn.setTextColor(0xFFFFFFFF);
                            GradientDrawable bg = new GradientDrawable();
                            bg.setColor(0xFF9C63F5);
                            bg.setCornerRadius(dpToPx(8));
                            btn.setBackground(bg);
                            if (option.contains("직접 입력")) {
                                btn.setOnClickListener(v -> {
                                    binding.layoutOptions.setVisibility(View.GONE);
                                    binding.etFreeAnswer.setText("");
                                    binding.etFreeAnswer.setHint("감정을 직접 입력해 주세요");
                                    binding.layoutFreeAnswer.setVisibility(View.VISIBLE);
                                    binding.btnSubmitFreeAnswer.setOnClickListener(sv -> {
                                        String answer = binding.etFreeAnswer.getText().toString().trim();
                                        if (answer.isEmpty()) { toast("내용을 입력해 주세요"); return; }
                                        submitClarification(answer);
                                    });
                                });
                            } else {
                                btn.setOnClickListener(v -> submitClarification(option));
                            }
                            binding.layoutOptions.addView(btn);
                        }
                    } else {
                        binding.layoutOptions.setVisibility(View.GONE);
                        binding.layoutFreeAnswer.setVisibility(View.VISIBLE);
                        binding.etFreeAnswer.setText("");
                        binding.btnSubmitFreeAnswer.setOnClickListener(v -> {
                            String answer = binding.etFreeAnswer.getText().toString().trim();
                            if (answer.isEmpty()) { toast("내용을 입력해 주세요"); return; }
                            submitClarification(answer);
                        });
                    }

                    animateIn(binding.layoutInterrupt);
                })
                .start();
    }

    private void submitClarification(String answer) {
        binding.layoutOptions.setVisibility(View.GONE);
        binding.layoutFreeAnswer.setVisibility(View.GONE);
        binding.progressInterrupt.setVisibility(View.VISIBLE);

        ApiClient.getApiService().clarifyEmotionReport(threadId, new ClarifyRequest(answer))
                .enqueue(new Callback<ApiResponse<ReportStatusResponse>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                            @NonNull Response<ApiResponse<ReportStatusResponse>> response) {
                        if (!isAdded()) return;
                        showState(STATE_LOADING);
                        startPolling();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                            @NonNull Throwable t) {
                        if (!isAdded()) return;
                        binding.layoutOptions.setVisibility(View.VISIBLE);
                        binding.progressInterrupt.setVisibility(View.GONE);
                        toast("전송에 실패했어요. 다시 시도해 주세요.");
                    }
                });
    }

    // ──────────────────────── Result ────────────────────────

    private void showResult(ReportStatusResponse data) {
        stopPolling();
        binding.tvTitle.setText(userName + " 님의 이번 주 감정");

        // 로딩 페이드아웃 → 카드 슬라이드인 → 피드백 슬라이드인
        binding.layoutLoading.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (!isAdded()) return;
                    binding.layoutLoading.setVisibility(View.GONE);
                    binding.layoutLoading.setAlpha(1f);

                    if (data.emotionReport != null) {
                        populateEmotionReport(data.emotionReport);
                        binding.tvEmotionChevron.setRotation(180f); // 처음엔 펼쳐진 상태
                        animateIn(binding.cardEmotion);
                    }

                    // 피드백은 카드보다 약간 늦게 등장
                    binding.layoutFeedback.postDelayed(() -> {
                        if (isAdded()) animateIn(binding.layoutFeedback);
                    }, 180);
                })
                .start();
    }

    private void populateEmotionReport(EmotionReportData emotionData) {
        binding.tvEmotionPeriod.setText(emotionPeriod);
        // 타이핑 효과로 리포트 본문 표시
        typewriterEffect(binding.tvEmotionContent, emotionData.content != null ? emotionData.content : "");

        if (emotionData.emotionFrequency != null && !emotionData.emotionFrequency.isEmpty()) {
            populateEmotionBars(emotionData.emotionFrequency);
        }
        if (emotionData.emotionByDay != null) {
            populateEmotionByDay(emotionData.emotionByDay);
        }
    }

    private void populateEmotionBars(Map<String, Integer> emotionFreq) {
        binding.layoutEmotionBars.removeAllViews();

        int max = 1;
        for (int v : emotionFreq.values()) {
            if (v > max) max = v;
        }

        int colorIdx = 0;
        for (Map.Entry<String, Integer> entry : emotionFreq.entrySet()) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(com.example.plog.R.layout.item_emotion_bar,
                            binding.layoutEmotionBars, false);

            ((TextView) row.findViewById(com.example.plog.R.id.tvEmotion)).setText(entry.getKey());
            ((TextView) row.findViewById(com.example.plog.R.id.tvCount)).setText(entry.getValue() + "회");

            ProgressBar bar = row.findViewById(com.example.plog.R.id.progressEmotion);
            bar.setMax(1000);
            bar.setProgress(0);
            bar.setProgressTintList(
                    ColorStateList.valueOf(EMOTION_COLORS[colorIdx % EMOTION_COLORS.length]));

            binding.layoutEmotionBars.addView(row);

            // 1000 기준으로 비율 스케일링 → 부드럽게 연속 차오름
            final int target = entry.getValue() * 1000 / max;
            final int idx = colorIdx;
            ValueAnimator anim = ValueAnimator.ofInt(0, target);
            anim.setDuration(900);
            anim.setStartDelay((long) idx * 150);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(a -> {
                if (isAdded()) bar.setProgress((int) a.getAnimatedValue());
            });
            anim.start();

            colorIdx++;
        }
    }

    private void populateEmotionByDay(List<EmotionByDay> emotionByDay) {
        binding.layoutEmotionByDay.removeAllViews();

        for (EmotionByDay item : emotionByDay) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(com.example.plog.R.layout.item_emotion_day,
                            binding.layoutEmotionByDay, false);

            ((TextView) row.findViewById(com.example.plog.R.id.tvDay)).setText(item.day);
            ((TextView) row.findViewById(com.example.plog.R.id.tvDate)).setText(item.date);

            TextView tvDayEmotion = row.findViewById(com.example.plog.R.id.tvDayEmotion);
            if (item.emotions != null && !item.emotions.isEmpty()) {
                tvDayEmotion.setText(String.join(" · ", item.emotions));
                tvDayEmotion.setAlpha(1f);
            } else {
                tvDayEmotion.setText("기록 없음");
                tvDayEmotion.setAlpha(0.4f);
            }

            binding.layoutEmotionByDay.addView(row);
        }
    }

    // ──────────────────────── Feedback ────────────────────────

    private void submitFeedback() {
        if (selectedRating == 0) { toast("별점을 선택해 주세요"); return; }

        String comment = binding.etFeedback.getText().toString().trim();
        binding.btnFeedback.setEnabled(false);

        ApiClient.getApiService()
                .submitEmotionReportFeedback(threadId,
                        new ReportFeedbackRequest(selectedRating, comment))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (!isAdded()) return;
                        binding.btnFeedback.setEnabled(true);
                        showFeedbackSuccess();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        binding.btnFeedback.setEnabled(true);
                        toast("피드백 전송에 실패했어요");
                    }
                });
    }

    private void showFeedbackSuccess() {
        binding.layoutFeedbackForm.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (!isAdded()) return;
                    binding.layoutFeedbackForm.setVisibility(View.GONE);
                    binding.layoutFeedbackForm.setAlpha(1f);
                    animateIn(binding.layoutFeedbackSuccess);
                })
                .start();
    }

    // ──────────────────────── Helpers ────────────────────────

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

}
