package com.example.plog.ui.report;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.plog.databinding.FragmentPlaceReportBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.ClarifyRequest;
import com.example.plog.model.GenerateReportRequest;
import com.example.plog.model.InterruptPayload;
import com.example.plog.model.PlaceEntry;
import com.example.plog.model.PlaceReportData;
import com.example.plog.model.ReportFeedbackRequest;
import com.example.plog.model.ReportStatusResponse;
import com.example.plog.network.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaceReportFragment extends Fragment {

    private static final int STATE_LOADING = 0;
    private static final int STATE_INTERRUPTED = 1;
    private static final int STATE_DONE = 2;

    private FragmentPlaceReportBinding binding;
    private String threadId;
    private String userName = "사용자";

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    // ──────────────────────── Lifecycle ────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPlaceReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvPlaces.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPlaces.setNestedScrollingEnabled(false);

        binding.btnClose.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());
        binding.headerPlace.setOnClickListener(v ->
                animateAccordion(binding.contentPlace, binding.tvPlaceChevron));
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
            binding.cardPlace.setVisibility(View.GONE);
            binding.layoutFeedback.setVisibility(View.GONE);
        }
    }

    // ──────────────────────── Generate & Poll ────────────────────────

    private void generateReport() {
        showState(STATE_LOADING);

        ApiClient.getApiService().generatePlaceReport(new GenerateReportRequest())
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
    }

    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || threadId == null) return;

                ApiClient.getApiService().getPlaceReportStatus(threadId)
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
                                                ? data.message : "장소 분석 중 오류가 발생했어요");
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

        ApiClient.getApiService().clarifyPlaceReport(threadId, new ClarifyRequest(answer))
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
        binding.tvTitle.setText(userName + " 님의 이번 달 장소");

        // 로딩 페이드아웃 → 카드 슬라이드인 → 피드백 슬라이드인
        binding.layoutLoading.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (!isAdded()) return;
                    binding.layoutLoading.setVisibility(View.GONE);
                    binding.layoutLoading.setAlpha(1f);

                    if (data.placeReport != null) {
                        populatePlaceReport(data.placeReport);
                        binding.tvPlaceChevron.setRotation(180f); // 처음엔 펼쳐진 상태
                        animateIn(binding.cardPlace);
                    }

                    // 피드백은 카드보다 약간 늦게 등장
                    binding.layoutFeedback.postDelayed(() -> {
                        if (isAdded()) animateIn(binding.layoutFeedback);
                    }, 180);
                })
                .start();
    }

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

    // ──────────────────────── Feedback ────────────────────────

    private void submitFeedback() {
        float rating = binding.ratingBar.getRating();
        if (rating == 0) { toast("별점을 선택해 주세요"); return; }

        String comment = binding.etFeedback.getText().toString().trim();
        binding.btnFeedback.setEnabled(false);

        ApiClient.getApiService()
                .submitPlaceReportFeedback(threadId,
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

        PlaceReportData place = new PlaceReportData();
        place.period = "2026년 5월";
        place.content = "(지역명)에 자주 방문하셨네요\n'카페(or태그명)'을 자주 가셨네요\n사진이 필요하다면 사진 첨부 등등\n이번달은 ---한 패턴을 보였습니다";
        place.topPhotoUrl = null;
        place.places = new java.util.ArrayList<>();
        PlaceEntry p1 = new PlaceEntry(); p1.placeName = "강남 카페거리"; p1.count = 5; p1.mainEmotion = "기쁨";
        PlaceEntry p2 = new PlaceEntry(); p2.placeName = "한강 공원"; p2.count = 2; p2.mainEmotion = "평온";
        PlaceEntry p3 = new PlaceEntry(); p3.placeName = "홍대"; p3.count = 1; p3.mainEmotion = "설렘";
        place.places.add(p1); place.places.add(p2); place.places.add(p3);

        ReportStatusResponse mock = new ReportStatusResponse();
        mock.status = "done";
        mock.userName = "사용자명";
        mock.placeReport = place;

        pollHandler.postDelayed(() -> {
            if (isAdded()) showResult(mock);
        }, 2000);
    }
}
