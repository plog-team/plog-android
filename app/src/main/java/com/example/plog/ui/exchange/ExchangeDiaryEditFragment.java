package com.example.plog.ui.exchange;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.plog.R;
import com.example.plog.databinding.FragmentDiaryEditBinding;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeDiaryApi;
import com.example.plog.network.dto.ExchangeDiaryRequest;
import com.example.plog.network.dto.ExchangeDiaryResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExchangeDiaryEditFragment extends Fragment {

    private FragmentDiaryEditBinding binding;
    private Long sessionId;
    private Long userId;
    private int dayNumber;
    private Long diaryId;

    public ExchangeDiaryEditFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiaryEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getLong("sessionId", 1L);
            userId = getArguments().getLong("userId", 1L);
            dayNumber = getArguments().getInt("dayNumber", 1);
            long did = getArguments().getLong("diaryId", -1L);
            if (did != -1L) diaryId = did;
        }

        if (diaryId != null) {
            binding.tvMode.setText("교환일기 수정");
            loadExistingDiary();
        } else {
            binding.tvMode.setText("교환일기 작성");
        }

        binding.btnClose.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        binding.btnSave.setOnClickListener(v -> showSavePopup());
    }

    private void loadExistingDiary() {
        ExchangeDiaryApi api = RetrofitClient.getClient().create(ExchangeDiaryApi.class);
        api.getDiaries(sessionId).enqueue(new Callback<List<ExchangeDiaryResponse>>() {
            @Override
            public void onResponse(Call<List<ExchangeDiaryResponse>> call, Response<List<ExchangeDiaryResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    for (ExchangeDiaryResponse d : response.body()) {
                        if (d.getId().equals(diaryId)) {
                            binding.etBody.setText(d.getContent());
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<ExchangeDiaryResponse>> call, Throwable t) {
                android.util.Log.e("ExchangeDiaryEdit", "기존 일기 로드 실패: " + t.getMessage());
            }
        });
    }

    private void showSavePopup() {
        View popupView = LayoutInflater.from(requireContext())
                .inflate(R.layout.popup_diary_save, null, false);
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                dp(190),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dp(8));

        CheckBox cbPublic = popupView.findViewById(R.id.cbPublic);
        CheckBox cbSecret = popupView.findViewById(R.id.cbSecret);
        TextView btnUpload = popupView.findViewById(R.id.btnUpload);
        TextView btnDismiss = popupView.findViewById(R.id.btnDismiss);

        cbPublic.setChecked(true);

        cbPublic.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) cbSecret.setChecked(false);
        });
        cbSecret.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) cbPublic.setChecked(false);
        });

        btnDismiss.setOnClickListener(v -> popupWindow.dismiss());
        btnUpload.setOnClickListener(v -> {
            popupWindow.dismiss();
            saveDiary();
        });

        popupWindow.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.END, dp(20), dp(72));
    }

    private void saveDiary() {
        String body = binding.etBody.getText().toString().trim();

        if (body.isEmpty()) {
            binding.etBody.requestFocus();
            Toast.makeText(requireContext(), "본문을 1자 이상 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        ExchangeDiaryApi api = RetrofitClient.getClient().create(ExchangeDiaryApi.class);

        if (diaryId != null) {
            // 수정
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("content", body);
            api.updateDiary(diaryId, requestBody).enqueue(new Callback<ExchangeDiaryResponse>() {
                @Override
                public void onResponse(Call<ExchangeDiaryResponse> call, Response<ExchangeDiaryResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "교환일기가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(binding.getRoot()).popBackStack();
                    } else {
                        Toast.makeText(requireContext(), "수정에 실패했어요.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ExchangeDiaryResponse> call, Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "서버 연결에 실패했어요.", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("ExchangeDiary", "수정 실패: " + t.getMessage());
                }
            });
        } else {
            // 새로 작성
            ExchangeDiaryRequest request = new ExchangeDiaryRequest(sessionId, userId, body, dayNumber);
            api.createDiary(request).enqueue(new Callback<ExchangeDiaryResponse>() {
                @Override
                public void onResponse(Call<ExchangeDiaryResponse> call, Response<ExchangeDiaryResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "교환일기가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(binding.getRoot()).popBackStack();
                    } else {
                        Toast.makeText(requireContext(), "저장에 실패했어요.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ExchangeDiaryResponse> call, Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "서버 연결에 실패했어요.", Toast.LENGTH_SHORT).show();
                    android.util.Log.e("ExchangeDiary", "저장 실패: " + t.getMessage());
                }
            });
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}