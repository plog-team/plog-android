package com.example.plog.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.example.plog.data.DiaryRepository;
import com.example.plog.databinding.FragmentHomeBinding;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeMatchApi;
import com.example.plog.network.api.ExchangeRoomApi;
import com.example.plog.network.dto.ExchangeMatchResponse;
import com.example.plog.network.dto.ExchangeRoomResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.cardDiaryBanner.setOnClickListener(v -> {
            DiaryRepository repository = new DiaryRepository(requireContext());
            String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
            int actionId = repository.getDiary(todayKey) == null
                    ? R.id.action_homeFragment_to_diaryEditFragment
                    : R.id.action_homeFragment_to_diaryDetailFragment;
            Navigation.findNavController(v).navigate(actionId);
        });

        binding.cardPlaceReport.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_place_report));

        binding.cardEmotionReport.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_emotion_report));

        binding.cardExchangeBanner.setOnClickListener(v -> checkActiveRoom());
    }

    private void checkActiveRoom() {
        ExchangeRoomApi api = RetrofitClient.getClient().create(ExchangeRoomApi.class);
        api.getActiveRoom(1L).enqueue(new Callback<ExchangeRoomResponse>() {
            @Override
            public void onResponse(Call<ExchangeRoomResponse> call, Response<ExchangeRoomResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    // 활성 교환방 있으면 matchedFragment로
                    Bundle bundle = new Bundle();
                    bundle.putLong("roomId", response.body().getId());
                    NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.matchedFragment, bundle);
                } else {
                    // 없으면 PENDING 매칭 확인
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
        ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        api.getMyActiveMatch(1L).enqueue(new Callback<ExchangeMatchResponse>() {
            @Override
            public void onResponse(Call<ExchangeMatchResponse> call, Response<ExchangeMatchResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    // PENDING 매칭 있으면 matchConfirmFragment로
                    Bundle bundle = new Bundle();
                    bundle.putLong("matchId", response.body().getId());
                    NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.matchConfirmFragment, bundle);
                } else {
                    // 아무것도 없으면 notMatchedFragment로
                    NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.notMatchedFragment);
                }
            }
            @Override
            public void onFailure(Call<ExchangeMatchResponse> call, Throwable t) {
                if (isAdded()) {
                    NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.notMatchedFragment);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}