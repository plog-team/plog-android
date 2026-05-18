package com.example.plog.ui.aiguide;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.example.plog.R;
import com.example.plog.databinding.FragmentAiGuideSessionBinding;
import com.example.plog.model.AnswerRequest;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.DraftResponse;
import com.example.plog.model.GuideQuestionDto;
import com.example.plog.model.SessionDetailResponse;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiGuideSessionFragment extends Fragment {

    public static final String ARG_SESSION_ID = "sessionId";
    /** Vision 요약 — 세션 진입 시 Entry에서 전달받아 칩으로 표시. */
    public static final String ARG_VISION_SCENE = "visionScene";
    public static final String ARG_VISION_MOOD = "visionMood";
    public static final String ARG_VISION_EMOTION = "visionEmotion";
    public static final String ARG_VISION_TIME = "visionTimeOfDay";
    public static final String ARG_VISION_OBJECTS = "visionObjectsCsv";
    private static final String TAG = "AiGuideSession";

    private FragmentAiGuideSessionBinding binding;
    private QuestionAnswerAdapter adapter;
    private final ApiService api = ApiClient.getApiService();
    private long sessionId = -1;

    public static Bundle argsOf(long sessionId) {
        Bundle b = new Bundle();
        b.putLong(ARG_SESSION_ID, sessionId);
        return b;
    }

    /** vision summary를 포함한 navigate args 빌드. */
    public static Bundle argsOf(long sessionId, String scene, String mood, String emotion, String timeOfDay, String objectsCsv) {
        Bundle b = new Bundle();
        b.putLong(ARG_SESSION_ID, sessionId);
        if (scene != null) b.putString(ARG_VISION_SCENE, scene);
        if (mood != null) b.putString(ARG_VISION_MOOD, mood);
        if (emotion != null) b.putString(ARG_VISION_EMOTION, emotion);
        if (timeOfDay != null) b.putString(ARG_VISION_TIME, timeOfDay);
        if (objectsCsv != null) b.putString(ARG_VISION_OBJECTS, objectsCsv);
        return b;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAiGuideSessionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getLong(ARG_SESSION_ID, -1);
        }
        if (sessionId <= 0) {
            Toast.makeText(getContext(), "잘못된 세션입니다", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        adapter = new QuestionAnswerAdapter();
        binding.vpQuestions.setAdapter(adapter);
        adapter.setViewPager(binding.vpQuestions);

        // 페이지 변경 시 진행 표시 + 이전/다음 버튼 상태 갱신
        binding.vpQuestions.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int total = adapter.getItemCount();
                if (binding != null) {
                    binding.tvProgress.setText((position + 1) + " / " + total);
                    binding.btnPrev.setEnabled(position > 0);
                    binding.btnNext.setEnabled(position < total - 1);
                }
            }
        });

        binding.btnPrev.setOnClickListener(v -> {
            int cur = binding.vpQuestions.getCurrentItem();
            if (cur > 0) binding.vpQuestions.setCurrentItem(cur - 1, true);
        });
        binding.btnNext.setOnClickListener(v -> {
            int cur = binding.vpQuestions.getCurrentItem();
            if (cur < adapter.getItemCount() - 1) binding.vpQuestions.setCurrentItem(cur + 1, true);
        });
        binding.btnDraft.setOnClickListener(v -> submitAndGenerateDraft());

        renderVisionChips();
        loadSession();
    }

    /** Vision 분석 결과 칩 (장면·키워드·감정) 렌더링. */
    private void renderVisionChips() {
        if (binding == null || getArguments() == null) return;
        Bundle a = getArguments();
        List<String> labels = new ArrayList<>();
        addChipLabel(labels, "장면", a.getString(ARG_VISION_SCENE));
        addChipLabel(labels, "분위기", a.getString(ARG_VISION_MOOD));
        addChipLabel(labels, "감정", a.getString(ARG_VISION_EMOTION));
        addChipLabel(labels, "시간", a.getString(ARG_VISION_TIME));
        String objectsCsv = a.getString(ARG_VISION_OBJECTS);
        if (objectsCsv != null && !objectsCsv.isEmpty()) {
            for (String obj : objectsCsv.split(",")) {
                String trimmed = obj.trim();
                if (!trimmed.isEmpty()) labels.add("# " + trimmed);
            }
        }
        if (labels.isEmpty()) return;
        binding.tvVisionLabel.setVisibility(View.VISIBLE);
        for (String label : labels) {
            Chip chip = new Chip(requireContext());
            chip.setText(label);
            chip.setClickable(false);
            chip.setCheckable(false);
            binding.cgVision.addView(chip);
        }
    }

    private static void addChipLabel(List<String> labels, String prefix, String value) {
        if (value != null && !value.isBlank()) {
            labels.add(prefix + ": " + value);
        }
    }

    private void loadSession() {
        showProgress(true);
        api.getAiSession(sessionId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SessionDetailResponse>> call, @NonNull Response<ApiResponse<SessionDetailResponse>> resp) {
                showProgress(false);
                if (!resp.isSuccessful() || resp.body() == null || resp.body().data == null) {
                    Toast.makeText(getContext(), "세션 조회 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                if (binding != null && resp.body().data.questions != null) {
                    adapter.setItems(resp.body().data.questions);
                    int total = adapter.getItemCount();
                    binding.tvProgress.setText("1 / " + total);
                    binding.btnPrev.setEnabled(false);
                    binding.btnNext.setEnabled(total > 1);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SessionDetailResponse>> call, @NonNull Throwable t) {
                showProgress(false);
                Toast.makeText(getContext(), "세션 조회 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitAndGenerateDraft() {
        Map<Long, String> answers = adapter.getAnswers();
        if (answers.isEmpty()) {
            generateDraft();
            return;
        }
        showProgress(true);
        binding.btnNext.setEnabled(false);

        AtomicInteger remaining = new AtomicInteger(answers.size());
        AtomicInteger fails = new AtomicInteger(0);
        for (Map.Entry<Long, String> e : answers.entrySet()) {
            api.answerQuestion(sessionId, e.getKey(), new AnswerRequest(e.getValue())).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<GuideQuestionDto>> call, @NonNull Response<ApiResponse<GuideQuestionDto>> resp) {
                    if (!resp.isSuccessful()) {
                        fails.incrementAndGet();
                        Log.w(TAG, "answer save failed qid=" + e.getKey() + " HTTP " + resp.code());
                    }
                    if (remaining.decrementAndGet() == 0) generateDraft();
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<GuideQuestionDto>> call, @NonNull Throwable t) {
                    fails.incrementAndGet();
                    Log.w(TAG, "answer save error qid=" + e.getKey() + " " + t.getMessage());
                    if (remaining.decrementAndGet() == 0) generateDraft();
                }
            });
        }
    }

    private void generateDraft() {
        api.generateDraft(sessionId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<DraftResponse>> call, @NonNull Response<ApiResponse<DraftResponse>> resp) {
                showProgress(false);
                if (binding != null) binding.btnNext.setEnabled(true);
                if (!resp.isSuccessful() || resp.body() == null || resp.body().data == null) {
                    Toast.makeText(getContext(), "초안 생성 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                if (getView() != null) {
                    Bundle args = AiGuideDraftFragment.argsOf(sessionId);
                    Navigation.findNavController(getView()).navigate(R.id.aiGuideDraftFragment, args);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<DraftResponse>> call, @NonNull Throwable t) {
                showProgress(false);
                if (binding != null) binding.btnNext.setEnabled(true);
                Toast.makeText(getContext(), "초안 생성 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgress(boolean show) {
        if (binding == null) return;
        binding.progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
