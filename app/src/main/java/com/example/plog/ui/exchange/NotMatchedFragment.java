package com.example.plog.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeMatchApi;
import com.example.plog.network.dto.ExchangeMatchListResponse;
import com.example.plog.network.dto.ExchangeMatchResponse;
import com.example.plog.network.dto.ExchangeRoomResponse;
import com.example.plog.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotMatchedFragment extends Fragment {

    private RecyclerView rvPendingMatches;
    private TextView tvNoPending;
    private PendingMatchAdapter adapter;

    public NotMatchedFragment() {}

    private long getMyUserId() {
        return new SessionManager(requireContext()).getUserId();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_not_matched, container, false);

        MaterialButton btnStartMatch = view.findViewById(R.id.btn_start_match);
        rvPendingMatches = view.findViewById(R.id.rvPendingMatches);
        tvNoPending = view.findViewById(R.id.tvNoPending);

        btnStartMatch.setOnClickListener(v ->
                NavHostFragment.findNavController(NotMatchedFragment.this).navigate(R.id.matchingFragment));

        adapter = new PendingMatchAdapter(new ArrayList<>(), this::acceptMatch, this::rejectMatch);
        rvPendingMatches.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPendingMatches.setAdapter(adapter);

        loadPendingMatches();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPendingMatches();
    }

    private void loadPendingMatches() {
        ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        api.getPendingMatches(getMyUserId()).enqueue(new Callback<ExchangeMatchListResponse>() {
            @Override
            public void onResponse(Call<ExchangeMatchListResponse> call, Response<ExchangeMatchListResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<ExchangeMatchResponse> list = response.body().getData();
                    adapter.updateList(list);
                    tvNoPending.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    rvPendingMatches.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<ExchangeMatchListResponse> call, Throwable t) {
                android.util.Log.e("NotMatched", "목록 로드 실패: " + t.getMessage());
            }
        });
    }

    private void acceptMatch(Long matchId) {
        String nickname = adapter.getNickname(matchId);
        ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        api.acceptMatch(matchId).enqueue(new Callback<ExchangeRoomResponse>() {
            @Override
            public void onResponse(Call<ExchangeRoomResponse> call, Response<ExchangeRoomResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("roomId", response.body().getId());
                    bundle.putString("partnerName", nickname);
                    NavHostFragment.findNavController(NotMatchedFragment.this)
                            .navigate(R.id.matchedFragment, bundle);
                }
            }
            @Override
            public void onFailure(Call<ExchangeRoomResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "수락 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectMatch(Long matchId) {
        ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        api.rejectMatch(matchId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "거절했어요", Toast.LENGTH_SHORT).show();
                loadPendingMatches();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(requireContext(), "거절 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
