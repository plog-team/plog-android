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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.plog.R;
import com.example.plog.databinding.FragmentAiGuideChatBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.ChatMessageDto;
import com.example.plog.model.DraftResponse;
import com.example.plog.model.SendChatRequest;
import com.example.plog.model.SendChatResponse;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiGuideChatFragment extends Fragment {

    public static final String ARG_SESSION_ID = "sessionId";
    public static final String ARG_FIRST_MSG = "firstMessage";

    private FragmentAiGuideChatBinding binding;
    private ChatMessageAdapter adapter;
    private final ApiService api = ApiClient.getApiService();
    private long sessionId = -1;

    public static Bundle argsOf(long sessionId, String firstMessage) {
        Bundle b = new Bundle();
        b.putLong(ARG_SESSION_ID, sessionId);
        b.putString(ARG_FIRST_MSG, firstMessage);
        return b;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAiGuideChatBinding.inflate(inflater, container, false);
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

        adapter = new ChatMessageAdapter();
        binding.rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChat.setAdapter(adapter);

        String firstMsg = getArguments() != null ? getArguments().getString(ARG_FIRST_MSG) : null;
        if (firstMsg != null && !firstMsg.isEmpty()) {
            adapter.addMessage(new ChatMessageDto("ASSISTANT", firstMsg));
        }

        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.btnDraft.setOnClickListener(v -> generateDraft());
    }

    private void sendMessage() {
        String text = binding.etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // 사용자 메시지를 즉시 표시하고 AI 응답 대기 중 typing placeholder 추가
        adapter.addMessage(new ChatMessageDto("USER", text));
        adapter.addTypingPlaceholder();
        scrollToBottom();
        binding.etInput.setText("");
        binding.btnSend.setEnabled(false);

        api.sendChat(sessionId, new SendChatRequest(text)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SendChatResponse>> call, @NonNull Response<ApiResponse<SendChatResponse>> resp) {
                if (binding != null) binding.btnSend.setEnabled(true);
                if (!resp.isSuccessful() || resp.body() == null || resp.body().data == null) {
                    if (binding != null) adapter.replaceLastAssistant("(전송 실패: HTTP " + resp.code() + ")");
                    Toast.makeText(getContext(), "전송 실패: HTTP " + resp.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                if (binding == null) return;
                SendChatResponse data = resp.body().data;
                adapter.replaceLastAssistant(data.assistantMessage);
                scrollToBottom();
                if (data.readyForDraft) {
                    // readyForDraft 이후에도 입력을 열어두어 사용자가 대화를 이어가거나 초안을 선택할 수 있도록 함
                    binding.btnDraft.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SendChatResponse>> call, @NonNull Throwable t) {
                if (binding != null) {
                    binding.btnSend.setEnabled(true);
                    adapter.replaceLastAssistant("(전송 실패: " + t.getMessage() + ")");
                }
                Toast.makeText(getContext(), "전송 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateDraft() {
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

    private void scrollToBottom() {
        if (binding != null && adapter.size() > 0) {
            binding.rvChat.smoothScrollToPosition(adapter.size() - 1);
        }
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
