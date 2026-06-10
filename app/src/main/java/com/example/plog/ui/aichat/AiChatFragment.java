package com.example.plog.ui.aichat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.plog.databinding.FragmentAiChatBinding;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;
import com.example.plog.network.aichat.AiChatMessageResponse;
import com.example.plog.network.aichat.AiChatSessionResponse;
import com.example.plog.network.aichat.AiChatSessionDetailResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiChatFragment extends Fragment {

    private FragmentAiChatBinding binding;
    private AiChatAdapter adapter;
    private ApiService apiService;
    private long sessionId = -1;
    private static final long USER_ID = 1L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getClient().create(ApiService.class);

        adapter = new AiChatAdapter();
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerChat.setAdapter(adapter);

        // 뒤로가기 시 endSession 호출
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        endSession();
                    }
                }
        );

        binding.btnClose.setOnClickListener(v -> endSession());

        Bundle args = getArguments();
        if (args != null && args.containsKey("resumeSessionId")) {
            // 이어하기: 기존 세션 불러오기
            sessionId = args.getLong("resumeSessionId");
            loadSession(sessionId);
        } else if (args != null && args.containsKey("year")) {
            // 달력에서 넘어온 날짜
            int year  = args.getInt("year");
            int month = args.getInt("month");
            int day   = args.getInt("day");
            String date = String.format("%04d-%02d-%02d", year, month, day);
            startSession("DIARY_ASSIST", date);
        } else {
            startSession("FREE_CHAT", null);
        }

        binding.btnSend.setOnClickListener(v -> {
            String message = binding.etMessage.getText().toString().trim();
            if (!message.isEmpty() && sessionId != -1) {
                adapter.addMessage(message, true);
                binding.etMessage.setText("");
                binding.recyclerChat.scrollToPosition(adapter.getItemCount() - 1);
                sendMessage(message);
            }
        });
    }

    // 기존 세션 이어하기
    private void loadSession(long sessionId) {
        apiService.getSessionDetail(sessionId).enqueue(new Callback<AiChatSessionDetailResponse>() {
            @Override
            public void onResponse(Call<AiChatSessionDetailResponse> call, Response<AiChatSessionDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 기존 메시지 복원
                    for (AiChatSessionDetailResponse.Message msg : response.body().data.data.messages) {
                        boolean isUser = "USER".equals(msg.sender);
                        adapter.addMessage(msg.content, isUser);
                    }
                    binding.recyclerChat.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
            @Override
            public void onFailure(Call<AiChatSessionDetailResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "대화 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 세션 종료 + 뒤로가기
    private void endSession() {
        if (sessionId == -1) {
            // endSession 호출 없이 바로 뒤로가기
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        apiService.endSession(sessionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void startSession(String type, @Nullable String date) {
        Call<AiChatSessionResponse> call;
        if (date != null) {
            call = apiService.startSessionWithDate(USER_ID, type, date);
        } else {
            call = apiService.startSession(USER_ID, type);
        }

        call.enqueue(new Callback<AiChatSessionResponse>() {
            @Override
            public void onResponse(Call<AiChatSessionResponse> call, Response<AiChatSessionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionId = response.body().data.data.sessionId;
                    String aiOpening = response.body().data.data.aiResponse;
                    if (aiOpening != null && !aiOpening.isEmpty()) {
                        adapter.addMessage(aiOpening, false);
                        binding.recyclerChat.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            }
            @Override
            public void onFailure(Call<AiChatSessionResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "세션 시작 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String message) {
        apiService.sendMessage(sessionId, message).enqueue(new Callback<AiChatMessageResponse>() {
            @Override
            public void onResponse(Call<AiChatMessageResponse> call, Response<AiChatMessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String aiResponse = response.body().data.data.aiResponse;
                    adapter.addMessage(aiResponse, false);
                    binding.recyclerChat.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
            @Override
            public void onFailure(Call<AiChatMessageResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "메시지 전송 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}