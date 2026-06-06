package com.example.plog.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.plog.R;
import com.example.plog.databinding.FragmentHomeBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.DiarySimpleResponse;
import com.example.plog.network.ApiClient;

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

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 메모리 누수 방지
    }
}
