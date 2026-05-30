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
import com.example.plog.data.DiaryEntry;
import com.example.plog.data.DiaryRepository;
import com.example.plog.databinding.FragmentDiaryEditBinding;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeDiaryApi;
import com.example.plog.network.dto.ExchangeDiaryRequest;
import com.example.plog.network.dto.ExchangeDiaryResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExchangeDiaryEditFragment extends Fragment {

    private FragmentDiaryEditBinding binding;
    private Long sessionId;
    private Long userId;
    private int dayNumber;

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
        }

        binding.tvMode.setText("교환일기 작성");
        binding.btnClose.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        binding.btnSave.setOnClickListener(v -> showSavePopup());
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
            if (saveDiary()) {
                popupWindow.dismiss();
                Navigation.findNavController(binding.getRoot()).popBackStack();
            }
        });

        popupWindow.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.END, dp(20), dp(72));
    }

    private boolean saveDiary() {
        String title = binding.etTitle.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (title.isEmpty()) {
            binding.etTitle.requestFocus();
            Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (body.isEmpty()) {
            binding.etBody.requestFocus();
            Toast.makeText(requireContext(), "본문을 1자 이상 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 로컬 저장 (key: "my_yyyy-MM-dd")
        String dateKey = "my_" + new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        DiaryEntry entry = new DiaryEntry();
        entry.setDate(dateKey);
        entry.setTitle(title);
        entry.setBody(body);
        entry.setWeather("");
        entry.setLocation("");
        new DiaryRepository(requireContext()).saveDiary(entry);

        // 백엔드 저장
        ExchangeDiaryApi api = RetrofitClient.getClient().create(ExchangeDiaryApi.class);
        ExchangeDiaryRequest request = new ExchangeDiaryRequest(sessionId, userId, body, dayNumber);
        api.createDiary(request).enqueue(new Callback<ExchangeDiaryResponse>() {
            @Override
            public void onResponse(Call<ExchangeDiaryResponse> call, Response<ExchangeDiaryResponse> response) {
                if (response.isSuccessful()) android.util.Log.d("ExchangeDiary", "저장 완료!");
            }
            @Override
            public void onFailure(Call<ExchangeDiaryResponse> call, Throwable t) {
                android.util.Log.e("ExchangeDiary", "저장 실패: " + t.getMessage());
            }
        });

        Toast.makeText(requireContext(), "교환일기가 저장되었습니다.", Toast.LENGTH_SHORT).show();
        return true;
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