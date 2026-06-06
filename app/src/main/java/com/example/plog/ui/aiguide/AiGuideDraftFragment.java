package com.example.plog.ui.aiguide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.plog.R;
import com.example.plog.databinding.FragmentAiGuideDraftBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.DiarySimpleResponse;
import com.example.plog.model.DiaryUpsertRequest;
import com.example.plog.model.FeedbackRequest;
import com.example.plog.model.SessionDetailResponse;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;
import com.example.plog.ui.util.TypingEffect;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiGuideDraftFragment extends Fragment {

    public static final String ARG_SESSION_ID = "sessionId";

    private FragmentAiGuideDraftBinding binding;
    private final ApiService api = ApiClient.getApiService();
    private long sessionId = -1;
    private String photoIdsCsv;
    private NavController navController;

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

        navController = Navigation.findNavController(view);

        if (getArguments() != null) {
            sessionId = getArguments().getLong(ARG_SESSION_ID, -1);
        }
        if (sessionId <= 0) {
            Toast.makeText(getContext(), "잘못된 세션입니다", Toast.LENGTH_SHORT).show();
            navController.popBackStack();
            return;
        }

        binding.etDraft.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding != null) binding.tvCharCount.setText(s.length() + "자");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btnConfirm.setEnabled(false);
        binding.btnConfirm.setOnClickListener(v -> confirm());
        binding.btnSubmitFeedback.setOnClickListener(v -> submitFeedback());

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
                photoIdsCsv = d.photoIdsCsv;

                if ("COMPLETED".equals(d.status)) {
                    navigateToHome();
                    return;
                }
                String draftText = d.draft == null ? "" : d.draft;
                binding.tvCharCount.setText(draftText.length() + "자");
                TypingEffect.apply(binding.etDraft, draftText, 18L, () -> {
                    if (binding != null) binding.btnConfirm.setEnabled(true);
                });
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
                if (binding != null) binding.btnConfirm.setText("이미 반영됨");
                String draftText = binding != null ? binding.etDraft.getText().toString() : "";
                saveDiaryAndGoHome(draftText);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SessionDetailResponse>> call, @NonNull Throwable t) {
                showProgress(false);
                if (binding != null) binding.btnConfirm.setEnabled(true);
                Toast.makeText(getContext(), "반영 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveDiaryAndGoHome(String draftText) {
        DiaryUpsertRequest req = new DiaryUpsertRequest();
        req.date = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        req.title = extractTitle(draftText);
        req.body = draftText;
        req.secret = false;
        req.bookmarked = false;
        req.representativePhotoIndex = 0;
        req.photoIds = parsePhotoIds(photoIdsCsv);

        api.saveDiary(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<DiarySimpleResponse>> call, @NonNull Response<ApiResponse<DiarySimpleResponse>> resp) {
                if (!resp.isSuccessful()) {
                    Toast.makeText(getContext(), "일기 저장 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                }
                Toast.makeText(getContext(), "일기에 반영되었습니다 ✓", Toast.LENGTH_SHORT).show();
                navigateToHome();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<DiarySimpleResponse>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "일기 저장 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
                navigateToHome();
            }
        });
    }

    private void navigateToHome() {
        if (navController != null) {
            navController.popBackStack(R.id.homeFragment, false);
        }
    }

    private String extractTitle(String draft) {
        if (draft == null || draft.trim().isEmpty()) return "AI 초안 일기";
        String firstLine = draft.trim().split("\n")[0].trim();
        if (firstLine.isEmpty()) return "AI 초안 일기";
        return firstLine.length() > 50 ? firstLine.substring(0, 50) : firstLine;
    }

    private List<Long> parsePhotoIds(String csv) {
        List<Long> ids = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) return ids;
        for (String s : csv.split(",")) {
            try { ids.add(Long.parseLong(s.trim())); }
            catch (Exception ignored) {}
        }
        return ids;
    }

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
