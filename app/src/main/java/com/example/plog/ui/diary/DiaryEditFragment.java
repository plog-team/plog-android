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
import android.provider.MediaStore;
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
import com.example.plog.databinding.FragmentDiaryEditBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.DiarySimpleResponse;
import com.example.plog.model.DiaryUpsertRequest;
import com.example.plog.network.ApiClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiaryEditFragment extends Fragment {
    private static final int MAX_PHOTO_COUNT = 10;
    private static final String DATE_LOADING = "날짜 불러오는 중...";
    private static final String LOCATION_LOADING = "위치 불러오는 중...";
    private static final String WEATHER_LOADING = "날씨 불러오는 중...";
    private static final String DATE_EMPTY = "날짜 정보 없음";
    private static final String LOCATION_EMPTY = "위치 정보 없음";
    private static final String WEATHER_EMPTY = "날씨 정보 없음";
    private static final String LOCATION_FAILED = "위치 정보를 불러오지 못했습니다";
    private static final String WEATHER_FAILED = "날씨 정보를 불러오지 못했습니다";

    private FragmentDiaryEditBinding binding;
    private PhotoViewModel photoViewModel;
    private long editingDiaryId = -1L;
    private String todayKey;
    private String selectedDiaryDateKey;
    private boolean isSecret;
    private boolean isBookmarked;
    private int representativePhotoIndex;
    private int previewPhotoIndex;
    private PopupWindow representativePopup;
    private final List<Uri>    selectedPhotos    = new ArrayList<>();
    /** DB photo.image_url 에 저장된 갤러리 URI 목록 — 사진 교체 시 소프트 삭제 대상 */
    private final List<String> activeGalleryUris = new ArrayList<>();
    private final List<Long> existingPhotoIds = new ArrayList<>();

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    openGalleryIntent());

    /** 원본 갤러리 URI와 DB photoId 매핑 */
    private final Map<String, Long> photoIdByGalleryUri = new HashMap<>();

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

                // 기존에 DB에 저장됐던 사진들 소프트 삭제
                for (String oldUri : activeGalleryUris) {
                    photoViewModel.removePhotoByUrl(oldUri);
                }
                activeGalleryUris.clear();
                existingPhotoIds.clear();
                selectedPhotos.clear();

                List<Uri> limited = uris.subList(0, Math.min(uris.size(), MAX_PHOTO_COUNT));
                for (Uri uri : limited) {
                    persistReadPermission(uri);
                    photoViewModel.processPhoto(uri);
                    activeGalleryUris.add(uri.toString()); // 갤러리 URI 추적
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
        photoViewModel = new ViewModelProvider(this).get(PhotoViewModel.class);
        editingDiaryId = getArguments() == null ? -1L : getArguments().getLong("diaryId", -1L);
        todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        selectedDiaryDateKey = todayKey;

        setupInitialState();
        setupListeners();
        observePhotoSaveResult();
    }

    /** 사진 저장 완료 시 원본 URI와 photoId를 매핑 */
    private void observePhotoSaveResult() {
        photoViewModel.getSavedPhotoResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            photoIdByGalleryUri.put(result.getUri(), result.getPhotoId());
        });
    }
    private void setupInitialState() {
        binding.tvDate.setText(formatDisplayDate(new Date()));
        binding.tvMode.setText(editingDiaryId > 0 ? "일기 수정" : "일기 작성");
        binding.tvPhotoCount.setText("0/" + MAX_PHOTO_COUNT + "(사진 개수)");
        updateBookmarkUi();
        if (editingDiaryId > 0) {
            loadExistingDiary();
        }
    }

    private void loadExistingDiary() {
        ApiClient.getApiService().getDiary(editingDiaryId)
                .enqueue(new Callback<ApiResponse<DiarySimpleResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<DiarySimpleResponse>> call,
                                           @NonNull Response<ApiResponse<DiarySimpleResponse>> response) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().data == null) {
                            Toast.makeText(requireContext(), "일기를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        bindExistingDiary(response.body().data);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<DiarySimpleResponse>> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "일기 조회 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindExistingDiary(DiarySimpleResponse diary) {
        selectedDiaryDateKey = isBlank(diary.date) ? todayKey : diary.date;
        binding.tvDate.setText(selectedDiaryDateKey);
        binding.etTitle.setText(diary.title == null ? "" : diary.title);
        binding.etBody.setText(diary.body == null ? "" : diary.body);
        binding.etLocation.setText(diary.location == null ? "" : diary.location);
        binding.etWeather.setText(diary.weather == null ? "" : diary.weather);
        isSecret = diary.secret;
        isBookmarked = diary.bookmarked;
        representativePhotoIndex = diary.representativePhotoIndex;
        previewPhotoIndex = representativePhotoIndex;
        existingPhotoIds.clear();
        if (diary.photoIds != null) {
            existingPhotoIds.addAll(diary.photoIds);
        }
        binding.tvPhotoCount.setText(existingPhotoIds.size() + "/" + MAX_PHOTO_COUNT + "(사진 개수)");
        updateBookmarkUi();
    }

    private void setupListeners() {
        binding.btnClose.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.photoPickerArea.setOnClickListener(v -> openPhotoPicker());
        binding.btnSave.setOnClickListener(v -> showSavePopup());
        setupAutoInputEditablePlaceholders();

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
        Bundle bundle = new Bundle();
        ArrayList<String> uriStrings = new ArrayList<>();
        for (Uri uri : selectedPhotos) {
            uriStrings.add(uri.toString());
        }
        bundle.putStringArrayList("photoUris", uriStrings);
        Navigation.findNavController(view)
                .navigate(R.id.action_diaryEditFragment_to_aiGuideEntryFragment, bundle);
    }

    private void setupAutoInputEditablePlaceholders() {
        binding.etLocation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                clearAutoInputPlaceholder(binding.etLocation);
            }
        });
        binding.etLocation.setOnClickListener(v -> clearAutoInputPlaceholder(binding.etLocation));
        binding.etWeather.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                clearAutoInputPlaceholder(binding.etWeather);
            }
        });
        binding.etWeather.setOnClickListener(v -> clearAutoInputPlaceholder(binding.etWeather));
    }

    private void clearAutoInputPlaceholder(TextView field) {
        String value = field.getText() == null ? "" : field.getText().toString().trim();
        if (isAutoInputPlaceholder(value)) {
            field.setText("");
        }
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
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType("image/*");
            intent.putExtra(
                    MediaStore.EXTRA_PICK_IMAGES_MAX,
                    Math.min(MAX_PHOTO_COUNT, MediaStore.getPickImagesMaxLimit())
            );
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        photoPicker.launch(intent);
    }

    private void persistReadPermission(Uri uri) {
        try {
            requireContext().getContentResolver()
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
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
        if (selectedPhotos.isEmpty()) {
            return;
        }

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

    private void readPhotoMetadata(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return;
            }

            ExifInterface exif = new ExifInterface(inputStream);
            float[] latLong = new float[2];
            if (exif.getLatLong(latLong) && isBlank(binding.etLocation.getText().toString())) {
                binding.etLocation.setText(String.format(Locale.KOREA, "위도 %.5f, 경도 %.5f", latLong[0], latLong[1]));
            }
        } catch (Exception ignored) {
        }
    }

    private Uri copyPhotoToLocalStorage(Uri sourceUri) {
        File photoDir = new File(requireContext().getFilesDir(), "diary_photos");
        if (!photoDir.exists() && !photoDir.mkdirs()) {
            return null;
        }

        File target = new File(photoDir, "diary_" + System.currentTimeMillis() + "_" + Math.abs(sourceUri.hashCode()) + ".jpg");
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(target)) {
            if (inputStream == null) {
                return null;
            }

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
            if (checked) {
                cbSecret.setChecked(false);
            }
        });
        cbSecret.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                cbPublic.setChecked(false);
            }
        });
        btnDismiss.setOnClickListener(v -> popupWindow.dismiss());
        btnUpload.setOnClickListener(v -> {
            isSecret = cbSecret.isChecked();
            saveDiary(popupWindow);
        });

        popupWindow.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.END, dp(20), dp(72));
    }
    /** 대표사진 URI로 서버 photoId를 조회한 뒤 자동입력 API를 호출 */
    private void applyAutoInputByGalleryUri(String galleryUri) {
        new Thread(() -> {
            try {
                requireActivity().runOnUiThread(() -> {
                    binding.tvDate.setText(DATE_LOADING);
                    binding.etLocation.setText(LOCATION_LOADING);
                    binding.etWeather.setText(WEATHER_LOADING);
                });

                Long serverPhotoId = null;

                // 서버 업로드 완료될 때까지 최대 10초 대기
                for (int i = 0; i < 20; i++) {
                    serverPhotoId = photoViewModel.getServerPhotoIdByImageUrl(galleryUri);

                    if (serverPhotoId != null) {
                        break;
                    }

                    Thread.sleep(500);
                }

                if (serverPhotoId == null) {
                    requireActivity().runOnUiThread(() -> {
                            binding.etLocation.setText(LOCATION_FAILED);
                            binding.etWeather.setText(WEATHER_FAILED);
                            Toast.makeText(
                                    requireContext(),
                                    "사진 분석 중입니다. 잠시 후 대표사진을 다시 선택해주세요.",
                                    Toast.LENGTH_SHORT
                            ).show();
                    });
                    return;
                }

                retrofit2.Response<com.example.plog.model.ApiResponse<com.example.plog.model.PhotoAutoInputContext>> response =
                        com.example.plog.network.ApiClient.getApiService()
                                .getPhotoAutoInput(serverPhotoId)
                                .execute();

                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().data == null) {

                    requireActivity().runOnUiThread(() -> {
                            binding.etLocation.setText(LOCATION_FAILED);
                            binding.etWeather.setText(WEATHER_FAILED);
                            Toast.makeText(
                                    requireContext(),
                                    "자동입력 정보를 불러오지 못했습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();
                    });
                    return;
                }

                com.example.plog.model.PhotoAutoInputContext context = response.body().data;

                requireActivity().runOnUiThread(() -> {
                    String dateText = context.date != null && !context.date.trim().isEmpty()
                            ? context.date
                            : DATE_EMPTY;
                    String dateKey = normalizeDateKey(context);
                    if (dateKey != null) {
                        selectedDiaryDateKey = dateKey;
                    }
                    binding.tvDate.setText(dateText);

                    binding.etLocation.setText(
                            context.locationHint != null && !context.locationHint.trim().isEmpty()
                                    ? context.locationHint
                                    : LOCATION_EMPTY
                    );

                    String weatherText;
                    if (context.weather != null && !context.weather.trim().isEmpty()) {
                        weatherText = context.weather;
                        if (context.temperature != null) {
                            weatherText += " / " + context.temperature + "℃";
                        }
                    } else {
                        weatherText = WEATHER_EMPTY;
                    }

                    binding.etWeather.setText(weatherText);

                    Toast.makeText(
                            requireContext(),
                            "자동입력 정보가 반영되었습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                        binding.etLocation.setText(LOCATION_FAILED);
                        binding.etWeather.setText(WEATHER_FAILED);
                        Toast.makeText(
                                requireContext(),
                                "자동입력 오류: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                });
            }
        }).start();
    }
    private void saveDiary(PopupWindow popupWindow) {
        String title = binding.etTitle.getText().toString().trim();
        String body = binding.etBody.getText().toString().trim();

        if (isBlank(title)) {
            binding.etTitle.requestFocus();
            Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isBlank(body)) {
            binding.etBody.requestFocus();
            Toast.makeText(requireContext(), "본문을 1자 이상 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Long> photoIds = new ArrayList<>();
        if (activeGalleryUris.isEmpty() && editingDiaryId > 0) {
            photoIds.addAll(existingPhotoIds);
        } else {
            for (String galleryUri : activeGalleryUris) {
                Long photoId = photoIdByGalleryUri.get(galleryUri);
                if (photoId == null) {
                    photoId = photoViewModel.getServerPhotoIdByImageUrl(galleryUri);
                }
                if (photoId != null) {
                    photoIds.add(photoId);
                }
            }
        }

        if (!activeGalleryUris.isEmpty() && photoIds.size() != activeGalleryUris.size()) {
            Toast.makeText(requireContext(), "사진 업로드가 아직 완료되지 않았습니다. 잠시 후 다시 저장해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        DiaryUpsertRequest request = new DiaryUpsertRequest();
        request.date = selectedDiaryDateKey == null ? todayKey : selectedDiaryDateKey;
        request.title = title;
        request.body = body;
        request.location = cleanInputValue(binding.etLocation.getText().toString());
        request.weather = cleanInputValue(binding.etWeather.getText().toString());
        request.secret = isSecret;
        request.bookmarked = isBookmarked;
        request.representativePhotoIndex = representativePhotoIndex;
        request.photoIds = photoIds;

        Call<ApiResponse<DiarySimpleResponse>> diaryCall = editingDiaryId > 0
                ? ApiClient.getApiService().updateDiary(editingDiaryId, request)
                : ApiClient.getApiService().saveDiary(request);

        diaryCall
                .enqueue(new Callback<ApiResponse<DiarySimpleResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<DiarySimpleResponse>> call,
                                           @NonNull Response<ApiResponse<DiarySimpleResponse>> response) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().data == null) {
                            Toast.makeText(requireContext(), "일기 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        popupWindow.dismiss();
                        Toast.makeText(requireContext(), "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                        Bundle args = new Bundle();
                        args.putLong("diaryId", response.body().data.diaryId);
                        Navigation.findNavController(binding.getRoot())
                                .navigate(R.id.action_diaryEditFragment_to_diaryDetailFragment, args);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<DiarySimpleResponse>> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "일기 저장 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateBookmarkUi() {
        binding.btnBookmark.setText(isBookmarked ? "★" : "☆");
        binding.btnBookmark.setTextColor(requireContext().getColor(isBookmarked ? R.color.purple_500 : R.color.gray_700));
    }

    private String formatDisplayDate(Date date) {
        return new SimpleDateFormat("yy/MM/dd(E)", Locale.KOREA).format(date);
    }

    private String normalizeDateKey(com.example.plog.model.PhotoAutoInputContext context) {
        if (context == null) {
            return null;
        }
        if (context.date != null && context.date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return context.date;
        }
        if (context.capturedAt != null && context.capturedAt.length() >= 10) {
            String capturedDate = context.capturedAt.substring(0, 10);
            if (capturedDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return capturedDate;
            }
        }
        return null;
    }

    private String cleanInputValue(String value) {
        String trimmed = value == null ? "" : value.trim();
        return isAutoInputPlaceholder(trimmed) ? "" : trimmed;
    }

    private boolean isAutoInputPlaceholder(String value) {
        return LOCATION_LOADING.equals(value)
                || WEATHER_LOADING.equals(value)
                || LOCATION_EMPTY.equals(value)
                || WEATHER_EMPTY.equals(value)
                || LOCATION_FAILED.equals(value)
                || WEATHER_FAILED.equals(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int clampPhotoIndex(int index) {
        if (selectedPhotos.isEmpty()) {
            return 0;
        }
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
