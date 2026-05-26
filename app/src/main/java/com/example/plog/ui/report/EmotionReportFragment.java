package com.example.plog.ui.report;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
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

    private FragmentEmotionReportBinding binding;
    private String threadId;
    private String userName = "사용자";
    private String emotionPeriod;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

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

        // ─── UI 테스트: showMockResult() 사용, 백엔드 완성 후 generateReport()로 교체 ───
        showMockResult();
        // generateReport();
    }

    @Override
    public void onDestroyView() {
        stopPolling();
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
        // 로딩 페이드아웃 후 인터럽트 카드 슬라이드인
        binding.layoutLoading.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    if (!isAdded()) return;
                    binding.layoutLoading.setVisibility(View.GONE);
                    binding.layoutLoading.setAlpha(1f);

                    binding.tvInterruptDate.setText(payload.date + " 일기");
                    binding.tvInterruptQuestion.setText(payload.question);
                    binding.layoutOptions.removeAllViews();
                    for (String option : payload.options) {
                        Button btn = new Button(requireContext());
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 0, dpToPx(8));
                        btn.setLayoutParams(params);
                        btn.setText(option);
                        btn.setOnClickListener(v -> submitClarification(option));
                        binding.layoutOptions.addView(btn);
                    }
                    binding.progressInterrupt.setVisibility(View.GONE);
                    animateIn(binding.layoutInterrupt);
                })
                .start();
    }

    private void submitClarification(String answer) {
        binding.layoutOptions.setVisibility(View.GONE);
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
        binding.tvEmotionContent.setText(emotionData.content != null ? emotionData.content : "");

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
            bar.setMax(max);
            bar.setProgress(entry.getValue());
            bar.setProgressTintList(
                    ColorStateList.valueOf(EMOTION_COLORS[colorIdx % EMOTION_COLORS.length]));

            binding.layoutEmotionBars.addView(row);
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
            if (item.emotion != null) {
                tvDayEmotion.setText(item.emotion);
            } else {
                tvDayEmotion.setText("기록 없음");
                tvDayEmotion.setAlpha(0.4f);
            }

            binding.layoutEmotionByDay.addView(row);
        }
    }

    // ──────────────────────── Feedback ────────────────────────

    private void submitFeedback() {
        float rating = binding.ratingBar.getRating();
        if (rating == 0) { toast("별점을 선택해 주세요"); return; }

        String comment = binding.etFeedback.getText().toString().trim();
        binding.btnFeedback.setEnabled(false);

        ApiClient.getApiService()
                .submitEmotionReportFeedback(threadId,
                        new ReportFeedbackRequest((int) rating, comment))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (!isAdded()) return;
                        binding.btnFeedback.setEnabled(true);
                        binding.etFeedback.setText("");
                        binding.ratingBar.setRating(0);
                        toast("피드백이 전달됐어요. 감사해요!");
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        binding.btnFeedback.setEnabled(true);
                        toast("피드백 전송에 실패했어요");
                    }
                });
    }

    // ──────────────────────── Helpers ────────────────────────

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    // ──────────────────────── Mock (백엔드 완성 후 제거) ────────────────────────

    private void showMockResult() {
        showState(STATE_LOADING);

        EmotionReportData emotion = new EmotionReportData();
        emotion.content = "이번주는 주로 기쁨을 느끼셨네요.\n가장 행복했던 날은 토요일\n카페에서 즐거운 하루를 보내셨군요\n전반적으로 긍정적인 한 주였습니다.";
        emotion.primaryEmotion = "기쁨";
        emotion.emotionFrequency = new java.util.LinkedHashMap<>();
        emotion.emotionFrequency.put("기쁨", 4);
        emotion.emotionFrequency.put("설렘", 2);
        emotion.emotionFrequency.put("평온", 1);
        emotion.emotionByDay = new java.util.ArrayList<>();
        String[] days = {"월", "화", "수", "목", "금", "토", "일"};
        String[] emos = {"기쁨", "슬픔", null, "설렘", "기쁨", "평온", "기쁨"};
        String[] dates = {"5월20일", "5월21일", "5월22일", "5월23일", "5월24일", "5월25일", "5월26일"};
        for (int i = 0; i < 7; i++) {
            EmotionByDay d = new EmotionByDay();
            d.day = days[i]; d.date = dates[i]; d.emotion = emos[i];
            emotion.emotionByDay.add(d);
        }

        ReportStatusResponse mock = new ReportStatusResponse();
        mock.status = "done";
        mock.userName = "사용자명";
        mock.emotionReport = emotion;

        pollHandler.postDelayed(() -> {
            if (isAdded()) showResult(mock);
        }, 2000);
    }
}
