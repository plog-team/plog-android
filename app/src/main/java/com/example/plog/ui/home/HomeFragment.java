package com.example.plog.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.plog.R;
import com.example.plog.databinding.FragmentHomeBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.DiarySimpleResponse;
import com.example.plog.network.ApiClient;
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
            String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
            ApiClient.getApiService().getDiaryByDate(todayKey)
                    .enqueue(new Callback<ApiResponse<DiarySimpleResponse>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<DiarySimpleResponse>> call,
                                               @NonNull Response<ApiResponse<DiarySimpleResponse>> response) {
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().data != null) {
                                Bundle args = new Bundle();
                                args.putLong("diaryId", response.body().data.diaryId);
                                Navigation.findNavController(v)
                                        .navigate(R.id.action_homeFragment_to_diaryDetailFragment, args);
                            } else {
                                Navigation.findNavController(v)
                                        .navigate(R.id.action_homeFragment_to_diaryEditFragment);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<DiarySimpleResponse>> call,
                                              @NonNull Throwable t) {
                            Toast.makeText(requireContext(), "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        binding.cardPlaceReport.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_place_report));

        binding.cardEmotionReport.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_emotion_report));

        binding.cardExchangeBanner.setOnClickListener(v -> checkActiveRoom());
    }

    private long getMyUserId() {
        return requireActivity()
                .getSharedPreferences("plog_prefs", Context.MODE_PRIVATE)
                .getInt("userId", 1);
    }

    private void checkActiveRoom() {
        ExchangeRoomApi api = RetrofitClient.getClient().create(ExchangeRoomApi.class);
        api.getActiveRoom(getMyUserId()).enqueue(new Callback<ExchangeRoomResponse>() {
            @Override
            public void onResponse(Call<ExchangeRoomResponse> call, Response<ExchangeRoomResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("roomId", response.body().getId());
                    NavHostFragment.findNavController(HomeFragment.this)
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
        ExchangeMatchApi api = RetrofitClient.getClient().create(ExchangeMatchApi.class);
        api.getMyActiveMatch(getMyUserId()).enqueue(new Callback<ExchangeMatchResponse>() {
            @Override
            public void onResponse(Call<ExchangeMatchResponse> call, Response<ExchangeMatchResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("matchId", response.body().getId());
                    NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.matchConfirmFragment, bundle);
                } else {
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