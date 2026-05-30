package com.example.plog.ui.exchange;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.util.Log;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeMatchApi;
import com.example.plog.network.dto.ExchangeRoomResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchConfirmFragment extends Fragment {

    private Call<ExchangeRoomResponse> acceptCall;

    private final List<List<String>> partnerPool = Arrays.asList(
            Arrays.asList("user1", "#영화", "#드라마", "#집순이"),
            Arrays.asList("user2", "#카페", "#사진", "#여행"),
            Arrays.asList("user3", "#개발", "#독서", "#고양이"),
            Arrays.asList("user4", "#맛집", "#베이킹", "#디저트"),
            Arrays.asList("user5", "#음악", "#기타", "#감성")
    );

    private int currentPartnerIndex = 0;

    public MatchConfirmFragment() {
        super(R.layout.fragment_match_confirm);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvMessage = view.findViewById(R.id.tvMatchMessage);
        TextView tvUserNickname = view.findViewById(R.id.tvUserNickname);
        Chip chipTag1 = view.findViewById(R.id.chipTag1);
        Chip chipTag2 = view.findViewById(R.id.chipTag2);
        Chip chipTag3 = view.findViewById(R.id.chipTag3);
        LinearLayout layoutButtons = view.findViewById(R.id.layoutButtons);
        MaterialButton btnAccept = view.findViewById(R.id.btnAccept);
        MaterialButton btnReject = view.findViewById(R.id.btnReject);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        setNewPartner(tvUserNickname, chipTag1, chipTag2, chipTag3);

        // 예 버튼
        btnAccept.setOnClickListener(v -> {
            tvMessage.setText("매칭 중...");
            layoutButtons.setVisibility(View.GONE);
            btnCancel.setVisibility(View.VISIBLE);

            ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);

            api.createMatch(new com.example.plog.network.dto.ExchangeMatchRequest(1L))
                    .enqueue(new Callback<com.example.plog.network.dto.ExchangeMatchResponse>() {
                        @Override
                        public void onResponse(Call<com.example.plog.network.dto.ExchangeMatchResponse> call,
                                               Response<com.example.plog.network.dto.ExchangeMatchResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Long newMatchId = response.body().getId();

                                acceptCall = api.acceptMatch(newMatchId);
                                acceptCall.enqueue(new Callback<ExchangeRoomResponse>() {
                                    @Override
                                    public void onResponse(Call<ExchangeRoomResponse> call,
                                                           Response<ExchangeRoomResponse> response) {
                                        if (response.isSuccessful() && response.body() != null) {
                                            Long roomId = response.body().getId();
                                            Bundle bundle = new Bundle();
                                            bundle.putString("partnerName", tvUserNickname.getText().toString());
                                            bundle.putLong("roomId", roomId);
                                            NavHostFragment.findNavController(MatchConfirmFragment.this)
                                                    .navigate(R.id.matchedFragment, bundle);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<ExchangeRoomResponse> call, Throwable t) {
                                        if (!call.isCanceled()) {
                                            Log.e("MatchConfirm", "수락 실패: " + t.getMessage());
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<com.example.plog.network.dto.ExchangeMatchResponse> call, Throwable t) {
                            Log.e("MatchConfirm", "신청 실패: " + t.getMessage());
                        }
                    });
        });

        // 다시 찾기 버튼
        btnReject.setOnClickListener(v -> {
            int nextIndex = currentPartnerIndex;
            if (partnerPool.size() > 1) {
                Random random = new Random();
                while (nextIndex == currentPartnerIndex) {
                    nextIndex = random.nextInt(partnerPool.size());
                }
            } else {
                nextIndex = 0;
            }
            currentPartnerIndex = nextIndex;
            setNewPartner(tvUserNickname, chipTag1, chipTag2, chipTag3);
        });

        // 취소 버튼
        btnCancel.setOnClickListener(v -> {
            if (acceptCall != null) {
                acceptCall.cancel();
                acceptCall = null;
            }
            tvMessage.setText("이 사용자에게 교환일기를 신청할까요?");
            layoutButtons.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.GONE);
        });
    }

    private void setNewPartner(TextView tvNickname, Chip chip1, Chip chip2, Chip chip3) {
        List<String> partnerData = partnerPool.get(currentPartnerIndex);
        tvNickname.setText(partnerData.get(0));
        chip1.setText(partnerData.get(1));
        chip2.setText(partnerData.get(2));
        chip3.setText(partnerData.get(3));
    }
}