package com.example.plog.ui.exchange;

import com.example.plog.network.dto.MatchRecommendResponse;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeMatchApi;
import com.example.plog.network.api.ExchangeRoomApi;
import com.example.plog.network.dto.ExchangeMatchRequest;
import com.example.plog.network.dto.ExchangeMatchResponse;
import com.example.plog.network.dto.ExchangeRoomResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchConfirmFragment extends Fragment {

    private Long pendingMatchId = null;
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private List<MatchRecommendResponse> recommendList = new ArrayList<>();
    private int currentPartnerIndex = 0;

    private TextView tvMessage;
    private TextView tvSimilarity;
    private LinearLayout layoutButtons;
    private MaterialButton btnCancel;

    public MatchConfirmFragment() {
        super(R.layout.fragment_match_confirm);
    }

    private long getMyUserId() {
        return requireActivity()
                .getSharedPreferences("plog_prefs", Context.MODE_PRIVATE)
                .getLong("userId", -1L);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvMessage = view.findViewById(R.id.tvMatchMessage);
        TextView tvUserNickname = view.findViewById(R.id.tvUserNickname);
        tvSimilarity = view.findViewById(R.id.tvSimilarity);
        Chip chipTag1 = view.findViewById(R.id.chipTag1);
        Chip chipTag2 = view.findViewById(R.id.chipTag2);
        Chip chipTag3 = view.findViewById(R.id.chipTag3);
        LinearLayout layoutButtons = view.findViewById(R.id.layoutButtons);
        this.layoutButtons = layoutButtons;
        MaterialButton btnAccept = view.findViewById(R.id.btnAccept);
        MaterialButton btnReject = view.findViewById(R.id.btnReject);
        btnCancel = view.findViewById(R.id.btnCancel);

        if (getArguments() != null) {
            long matchId = getArguments().getLong("matchId", -1L);
            if (matchId != -1L) {
                pendingMatchId = matchId;
                showWaitingUI();

                ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
                api.getMatch(matchId, getMyUserId()).enqueue(new Callback<ExchangeMatchResponse>() {
                    @Override
                    public void onResponse(Call<ExchangeMatchResponse> call, Response<ExchangeMatchResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            String nickname = response.body().getRequesterNickname();
                            if (nickname != null) tvUserNickname.setText(nickname);
                            List<String> categories = response.body().getTopCategories();
                            if (categories != null) {
                                chipTag1.setText(categories.size() > 0 ? "#" + categories.get(0) : "");
                                chipTag2.setText(categories.size() > 1 ? "#" + categories.get(1) : "");
                                chipTag3.setText(categories.size() > 2 ? "#" + categories.get(2) : "");
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<ExchangeMatchResponse> call, Throwable t) {
                        Log.e("MatchConfirm", "매칭 정보 조회 실패: " + t.getMessage());
                    }
                });

                startPolling(tvUserNickname);
                btnCancel.setOnClickListener(v -> cancelMatch());
                return;
            }

            recommendList = (ArrayList<MatchRecommendResponse>) getArguments().getSerializable("recommendList");
            if (recommendList != null && !recommendList.isEmpty()) {
                updatePartnerUI(tvUserNickname, chipTag1, chipTag2, chipTag3);
            }
        }

        btnAccept.setOnClickListener(v -> {
            MatchRecommendResponse partner = (recommendList != null && !recommendList.isEmpty())
                    ? recommendList.get(currentPartnerIndex) : null;
            Long targetUserId = partner != null ? partner.getUserId() : null;

            showWaitingUI();

            ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
            api.createMatch(new ExchangeMatchRequest(getMyUserId(), targetUserId))
                    .enqueue(new Callback<ExchangeMatchResponse>() {
                        @Override
                        public void onResponse(Call<ExchangeMatchResponse> call, Response<ExchangeMatchResponse> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null) {
                                pendingMatchId = response.body().getId();
                                Toast.makeText(requireContext(), "매칭 신청 완료!", Toast.LENGTH_SHORT).show();
                                startPolling(tvUserNickname);
                                btnCancel.setOnClickListener(cv -> cancelMatch());
                            } else {
                                Toast.makeText(requireContext(), "이미 진행 중인 매칭이 있어요.", Toast.LENGTH_SHORT).show();
                                showMatchUI();
                            }
                        }
                        @Override
                        public void onFailure(Call<ExchangeMatchResponse> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), "서버 연결에 실패했어요.", Toast.LENGTH_SHORT).show();
                            showMatchUI();
                        }
                    });
        });

        btnReject.setOnClickListener(v -> {
            if (recommendList != null && recommendList.size() > 1) {
                currentPartnerIndex = (currentPartnerIndex + 1) % recommendList.size();
                updatePartnerUI(tvUserNickname, chipTag1, chipTag2, chipTag3);
            }
        });
    }

    private void showWaitingUI() {
        tvMessage.setText("매칭 중... 상대방의 수락을 기다리고 있어요.");
        tvSimilarity.setVisibility(View.GONE);
        layoutButtons.setVisibility(View.GONE);
        btnCancel.setVisibility(View.VISIBLE);
    }

    private void showMatchUI() {
        tvMessage.setText("이 사용자에게 교환일기를 신청할까요?");
        tvSimilarity.setVisibility(View.VISIBLE);
        layoutButtons.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.GONE);
    }

    private void cancelMatch() {
        stopPolling();
        if (pendingMatchId != null) {
            ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
            api.rejectMatch(pendingMatchId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!isAdded()) return;
                    pendingMatchId = null;
                    if (recommendList != null && !recommendList.isEmpty()) {
                        showMatchUI();
                    } else {
                        NavHostFragment.findNavController(MatchConfirmFragment.this)
                                .navigate(R.id.matchingFragment);
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    if (!isAdded()) return;
                    pendingMatchId = null;
                    NavHostFragment.findNavController(MatchConfirmFragment.this)
                            .navigate(R.id.matchingFragment);
                }
            });
        }
    }

    private void startPolling(TextView tvUserNickname) {
        stopPolling();
        ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (pendingMatchId == null || !isAdded()) return;
                api.getMatch(pendingMatchId, getMyUserId()).enqueue(new Callback<ExchangeMatchResponse>() {
                    @Override
                    public void onResponse(Call<ExchangeMatchResponse> call, Response<ExchangeMatchResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            String status = response.body().getStatus();
                            if ("MATCHED".equals(status)) {
                                stopPolling();
                                ExchangeRoomApi roomApi = RetrofitClient.getClient().create(ExchangeRoomApi.class);
                                roomApi.getActiveRoom(getMyUserId()).enqueue(new Callback<ExchangeRoomResponse>() {
                                    @Override
                                    public void onResponse(Call<ExchangeRoomResponse> call, Response<ExchangeRoomResponse> response) {
                                        if (!isAdded()) return;
                                        Bundle bundle = new Bundle();
                                        if (response.isSuccessful() && response.body() != null) {
                                            bundle.putLong("roomId", response.body().getId());
                                        }
                                        NavHostFragment.findNavController(MatchConfirmFragment.this)
                                                .navigate(R.id.matchedFragment, bundle);
                                    }
                                    @Override
                                    public void onFailure(Call<ExchangeRoomResponse> call, Throwable t) {
                                        if (!isAdded()) return;
                                        NavHostFragment.findNavController(MatchConfirmFragment.this)
                                                .navigate(R.id.matchedFragment, new Bundle());
                                    }
                                });
                            } else if ("REJECTED".equals(status)) {
                                stopPolling();
                                Toast.makeText(requireContext(), "매칭이 거절됐어요.", Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(MatchConfirmFragment.this)
                                        .navigate(R.id.matchingFragment);
                            } else {
                                pollingHandler.postDelayed(pollingRunnable, 3000);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<ExchangeMatchResponse> call, Throwable t) {
                        if (isAdded()) pollingHandler.postDelayed(pollingRunnable, 3000);
                    }
                });
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void stopPolling() {
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }

    private void updatePartnerUI(TextView tvNickname, Chip chip1, Chip chip2, Chip chip3) {
        if (recommendList == null || recommendList.isEmpty()) return;
        MatchRecommendResponse partner = recommendList.get(currentPartnerIndex);
        tvNickname.setText(partner.getNickname());
        List<String> categories = partner.getTopCategories();
        chip1.setText(categories != null && categories.size() > 0 ? "#" + categories.get(0) : "");
        chip2.setText(categories != null && categories.size() > 1 ? "#" + categories.get(1) : "");
        chip3.setText(categories != null && categories.size() > 2 ? "#" + categories.get(2) : "");
        TextView tvSimilarity = requireView().findViewById(R.id.tvSimilarity);
        tvSimilarity.setText("나와 " + (int)(partner.getSimilarityScore() * 100) + "% 일치해요!");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
    }
}