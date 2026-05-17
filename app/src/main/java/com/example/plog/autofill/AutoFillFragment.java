package com.example.plog.autofill;

import android.Manifest;import android.app.Activity;import android.content.Intent;import android.content.pm.PackageManager;import android.net.Uri;import android.os.Build;import android.os.Bundle;import android.provider.MediaStore;import android.view.LayoutInflater;import android.view.View;import android.view.ViewGroup;import android.widget.EditText;import android.widget.ImageView;import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;import androidx.activity.result.contract.ActivityResultContracts;import androidx.annotation.NonNull;import androidx.annotation.Nullable;import androidx.appcompat.app.AlertDialog;import androidx.core.content.ContextCompat;import androidx.exifinterface.media.ExifInterface;import androidx.fragment.app.Fragment;

import com.example.plog.R;

import java.io.IOException;import java.io.InputStream;

public class AutoFillFragment extends Fragment {

    private TextView tvDate, tvWeather, tvLocation;
    private ImageView ivSelectedPhoto;
    private EditText etTitle, etContent;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;


    // 화면 생성 시 UI 초기화 및 권한/이미지 선택기 등록
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_auto_fill, container, false);

        initViews(view);              // XML UI 연결
        initImagePicker();            // 이미지 선택 결과 처리 등록
        initPermissionLauncher();     // 권한 요청 결과 처리 등록

        // 사진 클릭 시 권한 확인 후 선택 방식 다이얼로그 열기
        ivSelectedPhoto.setOnClickListener(
                v -> checkPermissionAndShowPickerDialog()
        );

        // 초기 화면 기본값 표시
        applyAutoFillData(
                new AutoFillData(
                        "사진을 선택해주세요",
                        "날씨 API 연결 전",
                        "위치 정보 없음",
                        null
                )
        );

        return view;
    }


    // XML 요소(TextView, ImageView 등) 연결
    private void initViews(View view) {
        tvDate = view.findViewById(R.id.tvDate);
        tvWeather = view.findViewById(R.id.tvWeather);
        tvLocation = view.findViewById(R.id.tvLocation);
        ivSelectedPhoto = view.findViewById(R.id.ivSelectedPhoto);
        etTitle = view.findViewById(R.id.etTitle);
        etContent = view.findViewById(R.id.etContent);
    }


    // 사진 위치 권한 확인 후 선택 방식 다이얼로그 실행
    private void checkPermissionAndShowPickerDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_MEDIA_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            permissionLauncher.launch(
                    new String[]{
                            Manifest.permission.ACCESS_MEDIA_LOCATION
                    }
            );

            return;
        }

        showImageSelectDialog();
    }


    // 권한 요청 결과 처리 후 선택 방식 다이얼로그 실행
    private void initPermissionLauncher() {
        permissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> showImageSelectDialog()
                );
    }


    // 갤러리 선택 / 파일 선택 다이얼로그 표시
    private void showImageSelectDialog() {
        String[] options = {
                "갤러리에서 선택",
                "파일에서 선택"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("사진 선택")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openGalleryPicker();
                    } else {
                        openFilePicker();
                    }
                })
                .show();
    }


    // 갤러리 앱으로 이미지 선택 실행
    private void openGalleryPicker() {
        Intent intent =
                new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                );

        intent.setType("image/*");

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        imagePickerLauncher.launch(intent);
    }


    // 파일 앱으로 이미지 선택 실행
    private void openFilePicker() {
        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        imagePickerLauncher.launch(intent);
    }


    // 갤러리/파일에서 선택한 이미지 URI 받아 EXIF 읽기 시작
    private void initImagePicker() {
        imagePickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() != Activity.RESULT_OK
                                    || result.getData() == null) {
                                return;
                            }

                            Uri imageUri = result.getData().getData();
                            if (imageUri == null) return;

                            // 파일 선택 방식일 경우 URI 권한 유지
                            // 갤러리 선택 방식은 영구 권한 저장이 안 될 수 있어서 예외 처리
                            try {
                                requireContext()
                                        .getContentResolver()
                                        .takePersistableUriPermission(
                                                imageUri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        );
                            } catch (SecurityException ignored) {
                                // ACTION_PICK으로 선택한 갤러리 URI는 여기로 올 수 있음
                            }

                            ivSelectedPhoto.setImageURI(imageUri);

                            // EXIF 정보 읽기
                            readExifAndApplyToUi(imageUri);
                        }
                );
    }


    // 선택한 이미지의 EXIF(촬영시간, GPS 등) 읽어서 UI 적용
    private void readExifAndApplyToUi(Uri imageUri) {
        try (
                InputStream inputStream =
                        requireContext()
                                .getContentResolver()
                                .openInputStream(imageUri)
        ) {
            if (inputStream == null) return;

            ExifInterface exif =
                    new ExifInterface(inputStream);

            // 촬영 날짜 읽기
            String dateTime =
                    exif.getAttribute(
                            ExifInterface.TAG_DATETIME_ORIGINAL
                    );

            if (dateTime == null) {
                dateTime =
                        exif.getAttribute(
                                ExifInterface.TAG_DATETIME
                        );
            }

            String displayDate =
                    formatExifDateTime(dateTime);

            // GPS 위치 읽기
            double[] latLong =
                    exif.getLatLong();

            String locationText =
                    latLong != null
                            ? String.format(
                            "위도: %.6f, 경도: %.6f",
                            latLong[0],
                            latLong[1]
                    )
                            : "GPS 위치 정보 없음";

            // 추출한 정보를 화면에 표시
            applyAutoFillData(
                    new AutoFillData(
                            displayDate,
                            "날씨 API 연결 전",
                            locationText,
                            imageUri
                    )
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // EXIF 날짜 형식을 보기 쉬운 문자열로 변환
    private String formatExifDateTime(String exifDateTime) {
        if (exifDateTime == null) return "촬영 날짜 정보 없음";

        try {
            String[] parts = exifDateTime.split(" ");
            String datePart = parts[0].replace(":", "/");

            if (parts.length > 1 && parts[1].length() >= 5) {
                return datePart + " " + parts[1].substring(0, 5);
            }

            return datePart;

        } catch (Exception e) {
            return exifDateTime;
        }
    }


    // 자동 추출한 날짜/위치/날씨 정보를 화면에 표시
    private void applyAutoFillData(AutoFillData data) {
        if (data == null) return;

        tvDate.setText("📅 " + data.date);
        tvWeather.setText("☀️ " + data.weather);
        tvLocation.setText("📍 " + data.location);

        if (data.imageUri != null) {
            ivSelectedPhoto.setImageURI(data.imageUri);
        }
    }


    // 자동 추출된 사진 정보(날짜, 위치 등) 저장용 클래스
    private static class AutoFillData {
        String date;
        String weather;
        String location;
        Uri imageUri;

        AutoFillData(
                String date,
                String weather,
                String location,
                Uri imageUri
        ) {
            this.date = date;
            this.weather = weather;
            this.location = location;
            this.imageUri = imageUri;
        }
    }

}