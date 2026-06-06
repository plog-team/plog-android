package com.example.plog.ui.aiguide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.plog.R;
import com.example.plog.databinding.FragmentAiGuideSessionBinding;
import com.example.plog.model.AnswerRequest;
import com.example.plog.model.AnswerResponse;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.DraftResponse;
import com.example.plog.model.SessionDetailResponse;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiGuideSessionFragment extends Fragment {

    public static final String ARG_SESSION_ID = "sessionId";
    public static final String ARG_VISION_SCENE = "visionScene";
    public static final String ARG_VISION_MOOD = "visionMood";
    public static final String ARG_VISION_EMOTION = "visionEmotion";
    public static final String ARG_VISION_TIME = "visionTimeOfDay";
    public static final String ARG_VISION_OBJECTS = "visionObjectsCsv";

    private FragmentAiGuideSessionBinding binding;
    private QuestionAnswerAdapter adapter;
    private final ApiService api = ApiClient.getApiService();
    private long sessionId = -1;
    private long currentQuestionId = -1;
    private boolean done = false;
    private boolean submitting = false;

    public static Bundle argsOf(long sessionId) {
        Bundle b = new Bundle();
        b.putLong(ARG_SESSION_ID, sessionId);
        return b;
    }

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
        adapter.setOnAnswerSelected(qid -> {
            if (qid == currentQuestionId) submitCurrentAnswer();
        });
        binding.vpQuestions.setUserInputEnabled(false);

        binding.btnPrev.setVisibility(View.GONE);
        binding.btnNext.setText("다음 질문");
        binding.btnNext.setOnClickListener(v -> submitCurrentAnswer());
        binding.btnDraft.setEnabled(false);
        binding.btnDraft.setOnClickListener(v -> generateDraft());

        renderVisionChips();
        loadSession();
    }

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
                if (binding == null) return;
                if (!resp.isSuccessful() || resp.body() == null || resp.body().data == null) {
                    Toast.makeText(getContext(), "세션 조회 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                if (resp.body().data.questions != null && !resp.body().data.questions.isEmpty()) {
                    adapter.setItems(resp.body().data.questions);
                    currentQuestionId = resp.body().data.questions
                            .get(resp.body().data.questions.size() - 1).questionId;
                    binding.tvProgress.setText("1번째 질문");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SessionDetailResponse>> call, @NonNull Throwable t) {
                showProgress(false);
                Toast.makeText(getContext(), "세션 조회 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitCurrentAnswer() {
        if (binding == null) return;
        if (submitting) return;
        if (currentQuestionId <= 0) return;
        String answer = adapter.getAnswers().get(currentQuestionId);
        if (answer == null || answer.trim().isEmpty()) {
            Toast.makeText(getContext(), "답변을 입력하거나 후보를 선택해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        submitting = true;
        showProgress(true);
        binding.btnNext.setEnabled(false);
        api.answerQuestion(sessionId, currentQuestionId, new AnswerRequest(answer.trim())).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AnswerResponse>> call, @NonNull Response<ApiResponse<AnswerResponse>> resp) {
                submitting = false;
                showProgress(false);
                if (binding == null) return;
                binding.btnNext.setEnabled(true);
                if (!resp.isSuccessful() || resp.body() == null || resp.body().data == null) {
                    Toast.makeText(getContext(), "답변 저장 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                AnswerResponse data = resp.body().data;
                if (data.done) {
                    done = true;
                    binding.btnNext.setVisibility(View.GONE);
                    binding.btnDraft.setEnabled(true);
                    Toast.makeText(getContext(), "충분히 답하셨어요. 이제 초안을 만들 수 있어요.", Toast.LENGTH_SHORT).show();
                } else if (data.nextQuestion != null) {
                    adapter.addItem(data.nextQuestion);
                    int last = adapter.getItemCount() - 1;
                    binding.vpQuestions.setCurrentItem(last, true);
                    currentQuestionId = data.nextQuestion.questionId;
                    binding.tvProgress.setText((data.answeredCount + 1) + "번째 질문");
                } else {
                    done = true;
                    binding.btnNext.setVisibility(View.GONE);
                    binding.btnDraft.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AnswerResponse>> call, @NonNull Throwable t) {
                submitting = false;
                showProgress(false);
                if (binding != null) binding.btnNext.setEnabled(true);
                Toast.makeText(getContext(), "답변 저장 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateDraft() {
        if (!done) {
            Toast.makeText(getContext(), "정보가 부족해서 초안을 작성할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress(true);
        binding.btnDraft.setEnabled(false);
        api.generateDraft(sessionId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<DraftResponse>> call, @NonNull Response<ApiResponse<DraftResponse>> resp) {
                showProgress(false);
                if (!resp.isSuccessful() || resp.body() == null || resp.body().data == null) {
                    if (binding != null) binding.btnDraft.setEnabled(true);
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
                if (binding != null) binding.btnDraft.setEnabled(true);
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
