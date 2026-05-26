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

import com.example.plog.databinding.FragmentAiGuideDraftBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.FeedbackRequest;
import com.example.plog.model.SessionDetailResponse;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;
import com.example.plog.ui.util.TypingEffect;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiGuideDraftFragment extends Fragment {

    public static final String ARG_SESSION_ID = "sessionId";

    private FragmentAiGuideDraftBinding binding;
    private final ApiService api = ApiClient.getApiService();
    private long sessionId = -1;

    public static Bundle argsOf(long sessionId) {
        Bundle b = new Bundle();
        b.putLong(ARG_SESSION_ID, sessionId);
        return b;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAiGuideDraftBinding.inflate(inflater, container, false);
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

        binding.etDraft.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding != null) binding.tvCharCount.setText(s.length() + "자");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btnConfirm.setOnClickListener(v -> confirm());
        binding.btnSubmitFeedback.setOnClickListener(v -> submitFeedback());

        // 메모리 섹션 Switch — 켜면 메모 입력 영역 표시
        binding.swMemory.setOnCheckedChangeListener((btn, isChecked) ->
                binding.memoryContent.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        loadDraft();
    }

    private void loadDraft() {
        showProgress(true);
        api.getAiSession(sessionId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SessionDetailResponse>> call, @NonNull Response<ApiResponse<SessionDetailResponse>> resp) {
                showProgress(false);
                if (binding == null) return;
                if (!resp.isSuccessful() || resp.body() == null || resp.body().data == null) {
                    Toast.makeText(getContext(), "초안 조회 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                SessionDetailResponse d = resp.body().data;
                // 초안을 한 글자씩 표시해 AI가 작성하는 느낌 연출
                String draftText = d.draft == null ? "" : d.draft;
                TypingEffect.apply(binding.etDraft, draftText, 18L);
                binding.tvCharCount.setText(draftText.length() + "자");
                if ("COMPLETED".equals(d.status)) {
                    binding.btnConfirm.setText("이미 반영됨");
                    binding.btnConfirm.setEnabled(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SessionDetailResponse>> call, @NonNull Throwable t) {
                showProgress(false);
                Toast.makeText(getContext(), "초안 조회 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirm() {
        if (binding == null) return;
        showProgress(true);
        binding.btnConfirm.setEnabled(false);
        api.confirmSession(sessionId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SessionDetailResponse>> call, @NonNull Response<ApiResponse<SessionDetailResponse>> resp) {
                showProgress(false);
                if (!resp.isSuccessful()) {
                    if (binding != null) binding.btnConfirm.setEnabled(true);
                    Toast.makeText(getContext(), "반영 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                if (binding != null) {
                    binding.btnConfirm.setText("이미 반영됨");
                }
                Toast.makeText(getContext(), "일기에 반영되었습니다 ✓", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SessionDetailResponse>> call, @NonNull Throwable t) {
                showProgress(false);
                if (binding != null) binding.btnConfirm.setEnabled(true);
                Toast.makeText(getContext(), "반영 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** 텍스트 코멘트만 받아 사용자 학습 가이드에 누적. */
    private void submitFeedback() {
        if (binding == null) return;
        String comment = binding.etFeedbackComment.getText().toString().trim();
        if (comment.isEmpty()) {
            Toast.makeText(getContext(), "반영할 내용을 한 줄이라도 적어 주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress(true);
        binding.btnSubmitFeedback.setEnabled(false);
        api.submitFeedback(sessionId, new FeedbackRequest(null, comment)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> resp) {
                showProgress(false);
                if (binding != null) binding.btnSubmitFeedback.setEnabled(true);
                if (resp.isSuccessful()) {
                    Toast.makeText(getContext(), "다음 일기 작성에 반영할게요 ✓", Toast.LENGTH_SHORT).show();
                    if (binding != null) binding.etFeedbackComment.setText("");
                } else {
                    Toast.makeText(getContext(), "저장 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                showProgress(false);
                if (binding != null) binding.btnSubmitFeedback.setEnabled(true);
                Toast.makeText(getContext(), "저장 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
