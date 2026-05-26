package com.example.plog.ui.report;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.databinding.FragmentReportBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.ClarifyRequest;
import com.example.plog.model.EmotionByDay;
import com.example.plog.model.EmotionReportData;
import com.example.plog.model.GenerateReportRequest;
import com.example.plog.model.InterruptPayload;
import com.example.plog.model.PlaceEntry;
import com.example.plog.model.PlaceReportData;
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

public class ReportFragment extends Fragment {

    private static final int STATE_LOADING = 0;
    private static final int STATE_INTERRUPTED = 1;
    private static final int STATE_DONE = 2;

    private static final int[] EMOTION_COLORS = {
            0xFF4CAF50, 0xFFFF9800, 0xFF2196F3,
            0xFFE91E63, 0xFF9C27B0, 0xFFF44336
    };

    private FragmentReportBinding binding;

    private String placeThreadId;
    private String emotionThreadId;
    private String userName = "사용자";
    private boolean placeDone = false;
    private boolean emotionDone = false;
    private boolean currentInterruptIsPlace = false;
    private String emotionPeriod;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable placePollRunnable;
    private Runnable emotionPollRunnable;

    // ──────────────────────── Lifecycle ────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupEmotionPeriod();
        binding.rvPlaces.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPlaces.setNestedScrollingEnabled(false);

        binding.btnClose.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());
        binding.headerPlace.setOnClickListener(v ->
                toggleAccordion(binding.contentPlace, binding.tvPlaceChevron));
        binding.headerEmotion.setOnClickListener(v ->
                toggleAccordion(binding.contentEmotion, binding.tvEmotionChevron));
        binding.btnFeedback.setOnClickListener(v -> submitFeedback());

        // ─── UI 테스트: showMockResult() 사용, 백엔드 완성 후 generateReports()로 교체 ───
        showMockResult();
        // generateReports();
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

    private void toggleAccordion(View content, TextView chevron) {
        boolean expanded = content.getVisibility() == View.VISIBLE;
        content.setVisibility(expanded ? View.GONE : View.VISIBLE);
        chevron.setText(expanded ? "∨" : "∧");
    }

    // ──────────────────────── Generate & Poll ────────────────────────

    private void generateReports() {
        placeDone = false;
        emotionDone = false;
        showState(STATE_LOADING);

        // 장소 리포트(월간) 생성 시작
        ApiClient.getApiService().generatePlaceReport(new GenerateReportRequest())
                .enqueue(new Callback<ApiResponse<ReportStatusResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                           @NonNull Response<ApiResponse<ReportStatusResponse>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            placeThreadId = response.body().data.threadId;
                            startPlacePolling();
                        } else {
                            toast("장소 리포트 생성을 시작할 수 없어요");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        toast("네트워크 오류가 발생했어요");
                    }
                });

        // 감정 리포트(주간) 생성 시작 — 병렬로 실행
        ApiClient.getApiService().generateEmotionReport(new GenerateReportRequest())
                .enqueue(new Callback<ApiResponse<ReportStatusResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                           @NonNull Response<ApiResponse<ReportStatusResponse>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null
                                && response.body().success) {
                            emotionThreadId = response.body().data.threadId;
                            startEmotionPolling();
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

    private void startPlacePolling() {
        placePollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || placeThreadId == null) return;

                ApiClient.getApiService().getPlaceReportStatus(placeThreadId)
                        .enqueue(new Callback<ApiResponse<ReportStatusResponse>>() {
                            @Override
                            public void onResponse(
                                    @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                    @NonNull Response<ApiResponse<ReportStatusResponse>> response) {
                                if (!isAdded()) return;
                                if (!response.isSuccessful() || response.body() == null
                                        || !response.body().success) {
                                    pollHandler.postDelayed(placePollRunnable, 2000);
                                    return;
                                }
                                ReportStatusResponse data = response.body().data;
                                if (data.userName != null) userName = data.userName;
                                switch (data.status) {
                                    case "done":
                                        placeDone = true;
                                        binding.cardPlace.setVisibility(View.VISIBLE);
                                        if (data.placeReport != null)
                                            populatePlaceReport(data.placeReport);
                                        checkAllDone();
                                        break;
                                    case "interrupted":
                                        currentInterruptIsPlace = true;
                                        stopPolling();
                                        showInterrupt(data.interruptPayload);
                                        break;
                                    case "error":
                                        placeDone = true;
                                        toast(data.message != null
                                                ? data.message : "장소 분석 중 오류가 발생했어요");
                                        checkAllDone();
                                        break;
                                    default:
                                        pollHandler.postDelayed(placePollRunnable, 2000);
                                }
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                    @NonNull Throwable t) {
                                if (isAdded()) pollHandler.postDelayed(placePollRunnable, 2000);
                            }
                        });
            }
        };
        pollHandler.post(placePollRunnable);
    }

    private void startEmotionPolling() {
        emotionPollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || emotionThreadId == null) return;

                ApiClient.getApiService().getEmotionReportStatus(emotionThreadId)
                        .enqueue(new Callback<ApiResponse<ReportStatusResponse>>() {
                            @Override
                            public void onResponse(
                                    @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                    @NonNull Response<ApiResponse<ReportStatusResponse>> response) {
                                if (!isAdded()) return;
                                if (!response.isSuccessful() || response.body() == null
                                        || !response.body().success) {
                                    pollHandler.postDelayed(emotionPollRunnable, 2000);
                                    return;
                                }
                                ReportStatusResponse data = response.body().data;
                                if (data.userName != null) userName = data.userName;
                                switch (data.status) {
                                    case "done":
                                        emotionDone = true;
                                        binding.cardEmotion.setVisibility(View.VISIBLE);
                                        if (data.emotionReport != null)
                                            populateEmotionReport(data.emotionReport);
                                        checkAllDone();
                                        break;
                                    case "interrupted":
                                        currentInterruptIsPlace = false;
                                        stopPolling();
                                        showInterrupt(data.interruptPayload);
                                        break;
                                    case "error":
                                        emotionDone = true;
                                        toast(data.message != null
                                                ? data.message : "감정 분석 중 오류가 발생했어요");
                                        checkAllDone();
                                        break;
                                    default:
                                        pollHandler.postDelayed(emotionPollRunnable, 2000);
                                }
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                                    @NonNull Throwable t) {
                                if (isAdded()) pollHandler.postDelayed(emotionPollRunnable, 2000);
                            }
                        });
            }
        };
        pollHandler.post(emotionPollRunnable);
    }

    private void stopPolling() {
        if (placePollRunnable != null) pollHandler.removeCallbacks(placePollRunnable);
        if (emotionPollRunnable != null) pollHandler.removeCallbacks(emotionPollRunnable);
    }

    private void checkAllDone() {
        if (!placeDone || !emotionDone) return;
        binding.tvTitle.setText(userName + " 님의 이번 달 활동");
        showState(STATE_DONE);
        binding.layoutFeedback.setVisibility(View.VISIBLE);
    }

    // ──────────────────────── Interrupt ────────────────────────

    private void showInterrupt(InterruptPayload payload) {
        showState(STATE_INTERRUPTED);

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
    }

    private void submitClarification(String answer) {
        binding.layoutOptions.setVisibility(View.GONE);
        binding.progressInterrupt.setVisibility(View.VISIBLE);

        String threadId = currentInterruptIsPlace ? placeThreadId : emotionThreadId;
        Call<ApiResponse<ReportStatusResponse>> call = currentInterruptIsPlace
                ? ApiClient.getApiService().clarifyPlaceReport(threadId, new ClarifyRequest(answer))
                : ApiClient.getApiService().clarifyEmotionReport(threadId, new ClarifyRequest(answer));

        call.enqueue(new Callback<ApiResponse<ReportStatusResponse>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<ReportStatusResponse>> call,
                    @NonNull Response<ApiResponse<ReportStatusResponse>> response) {
                if (!isAdded()) return;
                showState(STATE_LOADING);
                if (!placeDone) startPlacePolling();
                if (!emotionDone) startEmotionPolling();
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

    // ──────────────────────── Populate ────────────────────────

    private void populatePlaceReport(PlaceReportData placeData) {
        binding.tvPlacePeriod.setText(placeData.period != null ? placeData.period : "");
        binding.tvPlaceContent.setText(placeData.content != null ? placeData.content : "");

        if (placeData.topPhotoUrl != null) {
            binding.ivTopPlacePhoto.setVisibility(View.VISIBLE);
            Glide.with(requireContext())
                    .load(placeData.topPhotoUrl)
                    .centerCrop()
                    .into(binding.ivTopPlacePhoto);
        }

        if (placeData.places != null && !placeData.places.isEmpty()) {
            binding.rvPlaces.setAdapter(new PlaceCardAdapter(placeData.places));
        }
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
                    .inflate(R.layout.item_emotion_bar, binding.layoutEmotionBars, false);

            ((TextView) row.findViewById(R.id.tvEmotion)).setText(entry.getKey());
            ((TextView) row.findViewById(R.id.tvCount)).setText(entry.getValue() + "회");

            ProgressBar bar = row.findViewById(R.id.progressEmotion);
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
                    .inflate(R.layout.item_emotion_day, binding.layoutEmotionByDay, false);

            ((TextView) row.findViewById(R.id.tvDay)).setText(item.day);
            ((TextView) row.findViewById(R.id.tvDate)).setText(item.date);

            TextView tvDayEmotion = row.findViewById(R.id.tvDayEmotion);
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
        if (rating == 0) {
            toast("별점을 선택해 주세요");
            return;
        }

        String comment = binding.etFeedback.getText().toString().trim();
        binding.btnFeedback.setEnabled(false);
        ReportFeedbackRequest feedback = new ReportFeedbackRequest((int) rating, comment);

        // 두 파이프라인에 각각 피드백 전송 (fire-and-forget)
        if (placeThreadId != null) {
            ApiClient.getApiService()
                    .submitPlaceReportFeedback(placeThreadId, feedback)
                    .enqueue(new Callback<Void>() {
                        @Override public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {}
                        @Override public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {}
                    });
        }
        if (emotionThreadId != null) {
            ApiClient.getApiService()
                    .submitEmotionReportFeedback(emotionThreadId, feedback)
                    .enqueue(new Callback<Void>() {
                        @Override public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {}
                        @Override public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {}
                    });
        }

        if (isAdded()) {
            binding.btnFeedback.setEnabled(true);
            binding.etFeedback.setText("");
            binding.ratingBar.setRating(0);
            toast("피드백이 전달됐어요. 감사해요!");
        }
    }

    // ──────────────────────── State ────────────────────────

    private void showState(int state) {
        binding.layoutLoading.setVisibility(state == STATE_LOADING ? View.VISIBLE : View.GONE);
        binding.layoutInterrupt.setVisibility(state == STATE_INTERRUPTED ? View.VISIBLE : View.GONE);
        if (state == STATE_LOADING || state == STATE_INTERRUPTED) {
            binding.layoutFeedback.setVisibility(View.GONE);
        }
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
        placeDone = false;
        emotionDone = false;
        showState(STATE_LOADING);

        // 장소 리포트: 1.5초 후 완료
        PlaceReportData place = new PlaceReportData();
        place.period = "2026년 5월";
        place.content = "(지역명)에 자주 방문하셨네요\n'카페(or태그명)'을 자주 가셨네요\n사진이 필요하다면 사진 첨부 등등\n이번주는 ---한 패턴을 보였습니다";
        place.topPhotoUrl = null;
        place.places = new java.util.ArrayList<>();
        PlaceEntry p1 = new PlaceEntry(); p1.placeName = "강남 카페거리"; p1.count = 5; p1.mainEmotion = "기쁨";
        PlaceEntry p2 = new PlaceEntry(); p2.placeName = "한강 공원"; p2.count = 2; p2.mainEmotion = "평온";
        PlaceEntry p3 = new PlaceEntry(); p3.placeName = "홍대"; p3.count = 1; p3.mainEmotion = "설렘";
        place.places.add(p1); place.places.add(p2); place.places.add(p3);

        pollHandler.postDelayed(() -> {
            if (!isAdded()) return;
            placeDone = true;
            binding.cardPlace.setVisibility(View.VISIBLE);
            populatePlaceReport(place);
            checkAllDone();
        }, 1500);

        // 감정 리포트: 2.5초 후 완료 (장소보다 늦게 — 순차 표시 효과)
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

        pollHandler.postDelayed(() -> {
            if (!isAdded()) return;
            userName = "사용자명";
            emotionDone = true;
            binding.cardEmotion.setVisibility(View.VISIBLE);
            populateEmotionReport(emotion);
            checkAllDone();
        }, 2500);
    }
}
