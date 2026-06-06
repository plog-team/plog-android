package com.example.plog.ui.diary;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.plog.ui.photo.PhotoViewModel;

import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.data.DiaryEntry;
import com.example.plog.data.DiaryRepository;
import com.example.plog.databinding.FragmentDiaryEditBinding;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeDiaryApi;
import com.example.plog.network.dto.ExchangeDiaryRequest;
import com.example.plog.network.dto.ExchangeDiaryResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiaryEditFragment extends Fragment {
    private static final int MAX_PHOTO_COUNT = 10;

    private FragmentDiaryEditBinding binding;
    private DiaryRepository repository;
    private PhotoViewModel photoViewModel;
    private String todayKey;
    private boolean isSecret;
    private boolean isBookmarked;
    private int representativePhotoIndex;
    private int previewPhotoIndex;
    private PopupWindow representativePopup;
    private final List<Uri> selectedPhotos = new ArrayList<>();
    private final List<String> activeGalleryUris = new ArrayList<>();
    private final Map<String, Long> photoIdByGalleryUri = new HashMap<>();

    // 교환일기 모드 변수
    private boolean isExchange = false;
    private Long sessionId = null;
    private Long userId = null;
    private int dayNumber = 1;
    private Long diaryId = null;

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    openGalleryIntent());

    private final ActivityResultLauncher<Intent> photoPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;

                List<Uri> uris = new ArrayList<>();
                ClipData clip = result.getData().getClipData();
                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) uris.add(clip.getItemAt(i).getUri());
                } else if (result.getData().getData() != null) {
                    uris.add(result.getData().getData());
                }
                if (uris.isEmpty()) return;

                for (String oldUri : activeGalleryUris) {
                    photoViewModel.removePhotoByUrl(oldUri);
                }
                activeGalleryUris.clear();
                selectedPhotos.clear();

                List<Uri> limited = uris.subList(0, Math.min(uris.size(), MAX_PHOTO_COUNT));
                for (Uri uri : limited) {
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    photoViewModel.processPhoto(uri);
                    activeGalleryUris.add(uri.toString());
                    Uri localUri = copyPhotoToLocalStorage(uri);
                    selectedPhotos.add(localUri == null ? uri : localUri);
                }
                representativePhotoIndex = 0;
                previewPhotoIndex = 0;
                renderPhotos();

                if (!activeGalleryUris.isEmpty()) {
                    applyAutoInputByGalleryUri(activeGalleryUris.get(0));
                }
            });

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
        repository = new DiaryRepository(requireContext());
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        // 교환일기 모드 파라미터 받기
        if (getArguments() != null) {
            isExchange = getArguments().getBoolean("isExchange", false);
            sessionId = getArguments().getLong("sessionId", -1L);
            if (sessionId == -1L) sessionId = null;
            userId = getArguments().getLong("userId", -1L);
            if (userId == -1L) userId = null;
            dayNumber = getArguments().getInt("dayNumber", 1);
            long did = getArguments().getLong("diaryId", -1L);
            if (did != -1L) diaryId = did;
        }

        if (isExchange) {
            binding.tvMode.setText(diaryId != null ? "교환일기 수정" : "교환일기 작성");
        }

        setupInitialState();
        setupListeners();
        observePhotoSaveResult();
    }

    private void observePhotoSaveResult() {
        photoViewModel.getSavedPhotoResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            photoIdByGalleryUri.put(result.getUri(), result.getPhotoId());
        });
    }

    private void setupInitialState() {
        binding.tvDate.setText(formatDisplayDate(new Date()));

        DiaryEntry existingEntry = repository.getDiary(todayKey);
        if (existingEntry == null) {
            if (!isExchange) binding.tvMode.setText("일기 작성");
            binding.tvPhotoCount.setText("0/" + MAX_PHOTO_COUNT + "(사진 개수)");
            return;
        }

        if (!isExchange) binding.tvMode.setText("일기 수정");
        binding.etTitle.setText(existingEntry.getTitle());
        binding.etBody.setText(existingEntry.getBody());
        binding.etLocation.setText(existingEntry.getLocation());
        binding.etWeather.setText(existingEntry.getWeather());
        isSecret = existingEntry.isSecret();
        isBookmarked = existingEntry.isBookmarked();
        representativePhotoIndex = existingEntry.getRepresentativePhotoIndex();
        previewPhotoIndex = representativePhotoIndex;
        updateBookmarkUi();

        activeGalleryUris.clear();
        if (existingEntry.getGalleryPhotoUris() != null) {
            activeGalleryUris.addAll(existingEntry.getGalleryPhotoUris());
        }

        selectedPhotos.clear();
        for (String photoUri : existingEntry.getPhotoUris()) {
            selectedPhotos.add(Uri.parse(photoUri));
        }
        renderPhotos();

        if (!activeGalleryUris.isEmpty()) {
            int safeIndex = Math.max(0, Math.min(representativePhotoIndex, activeGalleryUris.size() - 1));
            applyAutoInputByGalleryUri(activeGalleryUris.get(safeIndex));
        }
    }

    private void setupListeners() {
        binding.btnClose.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.photoPickerArea.setOnClickListener(v -> openPhotoPicker());
        binding.btnSave.setOnClickListener(v -> showSavePopup());

        binding.btnBookmark.setOnClickListener(v -> {
            isBookmarked = !isBookmarked;
            updateBookmarkUi();
        });

        binding.btnAi.setOnClickListener(v -> {
            boolean show = binding.aiMenu.getVisibility() != View.VISIBLE;
            binding.aiMenu.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.btnAi.setText(show ? "×" : "+");
        });

        binding.btnAiQuestion.setOnClickListener(v -> openAiGuide(v));
        binding.btnAiDraft.setOnClickListener(v -> openAiGuide(v));
    }

    private void openAiGuide(View view) {
        binding.aiMenu.setVisibility(View.GONE);
        binding.btnAi.setText("+");
        Navigation.findNavController(view)
                .navigate(R.id.action_diaryEditFragment_to_aiGuideEntryFragment);
    }

    private void openPhotoPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_MEDIA_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION);
        } else {
            openGalleryIntent();
        }
    }

    private void openGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        photoPicker.launch(intent);
    }

    private void renderPhotos() {
        binding.tvPhotoCount.setText(selectedPhotos.size() + "/" + MAX_PHOTO_COUNT + "(사진 개수)");
        binding.photoStrip.removeAllViews();
        dismissRepresentativePopup();

        if (selectedPhotos.isEmpty()) {
            binding.ivRepresentativePhoto.setVisibility(View.GONE);
            binding.emptyPhotoState.setVisibility(View.VISIBLE);
            binding.photoScroll.setVisibility(View.GONE);
            return;
        }

        representativePhotoIndex = clampPhotoIndex(representativePhotoIndex);
        previewPhotoIndex = clampPhotoIndex(previewPhotoIndex);
        binding.ivRepresentativePhoto.setVisibility(View.VISIBLE);
        binding.emptyPhotoState.setVisibility(View.GONE);
        binding.photoScroll.setVisibility(View.VISIBLE);

        renderPreviewPhoto();

        for (int i = 0; i < selectedPhotos.size(); i++) {
            ImageView thumbnail = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(68), dp(68));
            params.setMarginEnd(dp(8));
            thumbnail.setLayoutParams(params);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setPadding(dp(2), dp(2), dp(2), dp(2));
            if (i == representativePhotoIndex) {
                thumbnail.setBackgroundResource(R.drawable.bg_thumbnail_selected);
            }

            int photoIndex = i;
            thumbnail.setOnClickListener(v -> {
                previewPhotoIndex = photoIndex;
                renderPreviewPhoto();
                showRepresentativePopup(v, photoIndex);
            });

            Glide.with(this)
                    .load(selectedPhotos.get(i))
                    .centerCrop()
                    .into(thumbnail);
            binding.photoStrip.addView(thumbnail);
        }
    }

    private void renderPreviewPhoto() {
        if (selectedPhotos.isEmpty()) return;

        Uri previewUri = selectedPhotos.get(previewPhotoIndex);
        binding.ivRepresentativePhoto.setImageDrawable(null);
        binding.ivRepresentativePhoto.setBackgroundResource(R.drawable.bg_photo_placeholder);

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(previewUri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                binding.ivRepresentativePhoto.setImageBitmap(bitmap);
                return;
            }
        } catch (Exception ignored) {
        }

        Glide.with(this)
                .load(previewUri)
                .centerCrop()
                .error(R.drawable.bg_photo_placeholder)
                .into(binding.ivRepresentativePhoto);
    }

    private void showRepresentativePopup(View anchor, int photoIndex) {
        dismissRepresentativePopup();

        TextView action = new TextView(requireContext());
        action.setWidth(dp(138));
        action.setHeight(dp(34));
        action.setGravity(Gravity.CENTER);
        action.setBackgroundResource(R.drawable.bg_representative_action);
        action.setText(photoIndex == representativePhotoIndex ? "대표사진입니다" : "대표사진으로 설정");
        action.setTextColor(requireContext().getColor(R.color.white));
        action.setTextSize(12);
        action.setEnabled(photoIndex != representativePhotoIndex);
        action.setOnClickListener(v -> {
            if (photoIndex == representativePhotoIndex) {
                dismissRepresentativePopup();
                return;
            }

            representativePhotoIndex = photoIndex;
            String selectedGalleryUri = activeGalleryUris.get(photoIndex);
            applyAutoInputByGalleryUri(selectedGalleryUri);
            dismissRepresentativePopup();
            renderPhotos();
            Toast.makeText(requireContext(), "대표사진이 변경되었습니다.", Toast.LENGTH_SHORT).show();
        });

        representativePopup = new PopupWindow(action, dp(138), dp(34), true);
        representativePopup.setOutsideTouchable(true);
        representativePopup.setElevation(dp(6));
        representativePopup.showAsDropDown(anchor, -dp(35), -dp(108), Gravity.CENTER);
    }

    private void dismissRepresentativePopup() {
        if (representativePopup != null && representativePopup.isShowing()) {
            representativePopup.dismiss();
        }
        representativePopup = null;
    }

    private Uri copyPhotoToLocalStorage(Uri sourceUri) {
        File photoDir = new File(requireContext().getFilesDir(), "diary_photos");
        if (!photoDir.exists() && !photoDir.mkdirs()) return null;

        File target = new File(photoDir, "diary_" + System.currentTimeMillis() + "_" + Math.abs(sourceUri.hashCode()) + ".jpg");
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(target)) {
            if (inputStream == null) return null;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            return Uri.fromFile(target);
        } catch (Exception ignored) {
            return null;
        }
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

        cbSecret.setChecked(isSecret);
        cbPublic.setChecked(!isSecret);

        cbPublic.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) cbSecret.setChecked(false);
        });
        cbSecret.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) cbPublic.setChecked(false);
        });

        btnDismiss.setOnClickListener(v -> popupWindow.dismiss());
        btnUpload.setOnClickListener(v -> {
            isSecret = cbSecret.isChecked();
            if (saveDiary()) {
                popupWindow.dismiss();
            }
        });

        popupWindow.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.END, dp(20), dp(72));
    }

    private void applyAutoInputByGalleryUri(String galleryUri) {
        new Thread(() -> {
            try {
                requireActivity().runOnUiThread(() -> {
                    binding.tvDate.setText("날짜 불러오는 중...");
                    binding.etLocation.setText("위치 불러오는 중...");
                    binding.etWeather.setText("날씨 불러오는 중...");
                });

                Long serverPhotoId = null;
                for (int i = 0; i < 20; i++) {
                    serverPhotoId = photoViewModel.getServerPhotoIdByImageUrl(galleryUri);
                    if (serverPhotoId != null) break;
                    Thread.sleep(500);
                }

                if (serverPhotoId == null) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "사진 분석 중입니다. 잠시 후 대표사진을 다시 선택해주세요.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                retrofit2.Response<com.example.plog.model.ApiResponse<com.example.plog.model.PhotoAutoInputContext>> response =
                        com.example.plog.network.ApiClient.getApiService()
                                .getPhotoAutoInput(serverPhotoId)
                                .execute();

                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "자동입력 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                com.example.plog.model.PhotoAutoInputContext context = response.body().data;
                requireActivity().runOnUiThread(() -> {
                    binding.tvDate.setText(context.date != null && !context.date.trim().isEmpty() ? context.date : "날짜 정보 없음");
                    binding.etLocation.setText(context.locationHint != null && !context.locationHint.trim().isEmpty() ? context.locationHint : "위치 정보 없음");
                    String weatherText = context.weather != null && !context.weather.trim().isEmpty() ? context.weather : "날씨 정보 없음";
                    if (context.weather != null && context.temperature != null) weatherText += " / " + context.temperature + "℃";
                    binding.etWeather.setText(weatherText);
                    Toast.makeText(requireContext(), "자동입력 정보가 반영되었습니다.", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "자동입력 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private boolean saveDiary() {
        String title = binding.etTitle.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (isBlank(title)) {
            binding.etTitle.requestFocus();
            Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isBlank(body)) {
            binding.etBody.requestFocus();
            Toast.makeText(requireContext(), "본문을 1자 이상 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 로컬 저장
        DiaryEntry entry = new DiaryEntry();
        entry.setDate(todayKey);
        entry.setTitle(title);
        entry.setBody(body);
        entry.setLocation(binding.etLocation.getText().toString().trim());
        entry.setWeather(binding.etWeather.getText().toString().trim());
        entry.setSecret(isSecret);
        entry.setBookmarked(isBookmarked);
        entry.setRepresentativePhotoIndex(representativePhotoIndex);

        List<String> photoUriStrings = new ArrayList<>();
        for (Uri uri : selectedPhotos) photoUriStrings.add(uri.toString());
        entry.setPhotoUris(photoUriStrings);
        entry.setGalleryPhotoUris(new ArrayList<>(activeGalleryUris));
        repository.saveDiary(entry);

        if (isExchange && sessionId != null) {
            // 교환일기 모드 - 백엔드에도 저장
            ExchangeDiaryApi api = RetrofitClient.getClient().create(ExchangeDiaryApi.class);
            if (diaryId != null) {
                // 수정
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("content", body);
                api.updateDiary(diaryId, requestBody).enqueue(new Callback<ExchangeDiaryResponse>() {
                    @Override
                    public void onResponse(Call<ExchangeDiaryResponse> call, Response<ExchangeDiaryResponse> response) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "교환일기가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(binding.getRoot()).popBackStack();
                    }
                    @Override
                    public void onFailure(Call<ExchangeDiaryResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "수정 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // 새로 작성
                ExchangeDiaryRequest request = new ExchangeDiaryRequest(sessionId, 1L, body, dayNumber);
                api.createDiary(request).enqueue(new Callback<ExchangeDiaryResponse>() {
                    @Override
                    public void onResponse(Call<ExchangeDiaryResponse> call, Response<ExchangeDiaryResponse> response) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "교환일기가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(binding.getRoot()).popBackStack();
                    }
                    @Override
                    public void onFailure(Call<ExchangeDiaryResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "저장 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            // 일반 일기 모드
            Toast.makeText(requireContext(), "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(binding.getRoot()).popBackStack(R.id.homeFragment, false);
        }

        return true;
    }

    private void updateBookmarkUi() {
        binding.btnBookmark.setText(isBookmarked ? "★" : "☆");
        binding.btnBookmark.setTextColor(requireContext().getColor(isBookmarked ? R.color.purple_500 : R.color.gray_700));
    }

    private String formatDisplayDate(Date date) {
        return new SimpleDateFormat("yy/MM/dd(E)", Locale.KOREA).format(date);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int clampPhotoIndex(int index) {
        if (selectedPhotos.isEmpty()) return 0;
        return Math.max(0, Math.min(index, selectedPhotos.size() - 1));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissRepresentativePopup();
        binding = null;
    }
}