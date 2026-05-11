package com.example.plog.ui.diary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
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
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.data.DiaryEntry;
import com.example.plog.data.DiaryRepository;
import com.example.plog.databinding.FragmentDiaryEditBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiaryEditFragment extends Fragment {
    private static final int MAX_PHOTO_COUNT = 10;

    private FragmentDiaryEditBinding binding;
    private DiaryRepository repository;
    private String todayKey;
    private boolean isSecret;
    private boolean isBookmarked;
    private int representativePhotoIndex;
    private int previewPhotoIndex;
    private PopupWindow representativePopup;
    private final List<Uri> selectedPhotos = new ArrayList<>();

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTO_COUNT), uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }

                selectedPhotos.clear();
                for (Uri uri : uris.subList(0, Math.min(uris.size(), MAX_PHOTO_COUNT))) {
                    Uri localUri = copyPhotoToLocalStorage(uri);
                    selectedPhotos.add(localUri == null ? uri : localUri);
                }
                representativePhotoIndex = 0;
                previewPhotoIndex = 0;
                readPhotoMetadata(uris.get(0));
                renderPhotos();
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
        todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        setupInitialState();
        setupListeners();
    }

    private void setupInitialState() {
        binding.tvDate.setText(formatDisplayDate(new Date()));

        DiaryEntry existingEntry = repository.getDiary(todayKey);
        if (existingEntry == null) {
            binding.tvMode.setText("일기 작성");
            binding.tvPhotoCount.setText("0/" + MAX_PHOTO_COUNT + "(사진 개수)");
            return;
        }

        binding.tvMode.setText("일기 수정");
        binding.etTitle.setText(existingEntry.getTitle());
        binding.etBody.setText(existingEntry.getBody());
        binding.etLocation.setText(existingEntry.getLocation());
        binding.etWeather.setText(existingEntry.getWeather());
        isSecret = existingEntry.isSecret();
        isBookmarked = existingEntry.isBookmarked();
        representativePhotoIndex = existingEntry.getRepresentativePhotoIndex();
        previewPhotoIndex = representativePhotoIndex;
        updateBookmarkUi();

        selectedPhotos.clear();
        for (String photoUri : existingEntry.getPhotoUris()) {
            selectedPhotos.add(Uri.parse(photoUri));
        }
        renderPhotos();
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

        binding.btnAiQuestion.setOnClickListener(v ->
                Toast.makeText(requireContext(), "AI 질문 생성 화면과 연결될 예정입니다.", Toast.LENGTH_SHORT).show());

        binding.btnAiDraft.setOnClickListener(v ->
                Toast.makeText(requireContext(), "AI 초안 생성 화면과 연결될 예정입니다.", Toast.LENGTH_SHORT).show());
    }

    private void openPhotoPicker() {
        photoPicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
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
            representativePhotoIndex = photoIndex;
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
            if (saveDiary()) {
                popupWindow.dismiss();
                Navigation.findNavController(binding.getRoot()).popBackStack(R.id.homeFragment, false);
            }
        });

        popupWindow.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.END, dp(20), dp(72));
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
        for (Uri uri : selectedPhotos) {
            photoUriStrings.add(uri.toString());
        }
        entry.setPhotoUris(photoUriStrings);

        repository.saveDiary(entry);
        Toast.makeText(requireContext(), "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show();
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
