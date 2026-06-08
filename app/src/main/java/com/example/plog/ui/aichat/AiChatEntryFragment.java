package com.example.plog.ui.aichat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.plog.R;
import com.example.plog.databinding.FragmentAiChatEntryBinding;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;
import com.example.plog.network.aichat.AiChatSessionListResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiChatEntryFragment extends Fragment {

    private FragmentAiChatEntryBinding binding;
    private ApiService apiService;                  // ← 추가
    private AiChatSessionAdapter sessionAdapter;    // ← 추가
    private static final long USER_ID = 1L;         // ← 추가

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatEntryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getClient().create(ApiService.class);

        sessionAdapter = new AiChatSessionAdapter();
        binding.recyclerSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSessions.setAdapter(sessionAdapter);

        sessionAdapter.setOnSessionClickListener(sessionId -> {
            Bundle args = new Bundle();
            args.putLong("resumeSessionId", sessionId);

            Navigation.findNavController(requireView())
                    .navigate(R.id.action_aiChatEntry_to_aiChatFragment, args);
        });

        binding.btnClose.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        binding.btnStartChat.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_aiChatEntry_to_aiChatOption)
        );

        loadSessions();
    }

    private void loadSessions() {
        apiService.getSessions(USER_ID).enqueue(new Callback<AiChatSessionListResponse>() {
            @Override
            public void onResponse(Call<AiChatSessionListResponse> call, Response<AiChatSessionListResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null) {
                    sessionAdapter.setSessions(response.body().data.data);
                }
            }
            @Override
            public void onFailure(Call<AiChatSessionListResponse> call, Throwable t) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}