package com.example.plog.ui.exchange;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeMatchApi;
import com.example.plog.network.api.ExchangeRoomApi;
import com.example.plog.network.api.MatchRecommendApi;
import com.example.plog.network.dto.ExchangeMatchResponse;
import com.example.plog.network.dto.ExchangeRoomResponse;
import com.example.plog.network.dto.MatchRecommendResponse;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchingFragment extends Fragment {

    public MatchingFragment() {
        super(R.layout.fragment_matching);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkActiveRoomFirst();
    }

    private void checkActiveRoomFirst() {
        ExchangeRoomApi roomApi = RetrofitClient.getClient().create(ExchangeRoomApi.class);
        roomApi.getActiveRoom(1L).enqueue(new Callback<ExchangeRoomResponse>() {
            @Override
            public void onResponse(Call<ExchangeRoomResponse> call, Response<ExchangeRoomResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("roomId", response.body().getId());
                    NavHostFragment.findNavController(MatchingFragment.this)
                            .navigate(R.id.matchedFragment, bundle);
                } else {
                    checkPendingMatch();
                }
            }
            @Override
            public void onFailure(Call<ExchangeRoomResponse> call, Throwable t) {
                if (isAdded()) checkPendingMatch();
            }
        });
    }

    private void checkPendingMatch() {
        ExchangeMatchApi matchApi = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        matchApi.getMyActiveMatch(1L).enqueue(new Callback<ExchangeMatchResponse>() {
            @Override
            public void onResponse(Call<ExchangeMatchResponse> call, Response<ExchangeMatchResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("matchId", response.body().getId());
                    NavHostFragment.findNavController(MatchingFragment.this)
                            .navigate(R.id.matchConfirmFragment, bundle);
                } else {
                    loadRecommendedUsers();
                }
            }
            @Override
            public void onFailure(Call<ExchangeMatchResponse> call, Throwable t) {
                if (isAdded()) loadRecommendedUsers();
            }
        });
    }

    private void loadRecommendedUsers() {
        MatchRecommendApi api = RetrofitClient.getClient().create(MatchRecommendApi.class);
        api.recommendMatches(1L).enqueue(new Callback<List<MatchRecommendResponse>>() {
            @Override
            public void onResponse(Call<List<MatchRecommendResponse>> call, Response<List<MatchRecommendResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("recommendList", new ArrayList<>(response.body()));
                    NavHostFragment.findNavController(MatchingFragment.this)
                            .navigate(R.id.matchConfirmFragment, bundle);
                } else {
                    // 추천 유저 없으면 그냥 notMatchedFragment로
                    NavHostFragment.findNavController(MatchingFragment.this)
                            .navigate(R.id.notMatchedFragment);
                }
            }
            @Override
            public void onFailure(Call<List<MatchRecommendResponse>> call, Throwable t) {
                if (isAdded()) {
                    Log.e("Matching", "사용자 검색 실패: " + t.getMessage());
                    NavHostFragment.findNavController(MatchingFragment.this)
                            .navigate(R.id.notMatchedFragment);
                }
            }
        });
    }
}