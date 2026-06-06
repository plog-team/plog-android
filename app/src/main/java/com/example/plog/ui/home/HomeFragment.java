package com.example.plog.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.plog.R;
import com.example.plog.data.DiaryRepository;
import com.example.plog.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 메모리 누수 방지
    }
}
