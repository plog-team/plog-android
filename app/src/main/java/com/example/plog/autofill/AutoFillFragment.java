/*
 * AutoFillFragment
 *
 * [기능]
 * - 사용자가 선택한 사진의 EXIF 정보를 읽어 자동입력 기능 수행
 * - 촬영 날짜, GPS 위치, 현재 날씨 정보를 자동 추출하여 화면에 표시
 *
 * [외부 API]
 * - Kakao Local API
 *   : 위도/경도를 주소 문자열로 변환
 *
 * - OpenWeather API
 *   : 위도/경도를 기반으로 현재 날씨 조회
 *
 * [사용 기술]
 * - EXIF Interface
 * - OkHttp
 * - JSON Parsing
 *
 * [외부 환경]
 * - 인터넷 연결 필요
 * - ACCESS_MEDIA_LOCATION 권한 사용
 * - API KEY는 Constants.java에서 관리
 *
 * [주의]
 * - API KEY 미설정 시 주소/날씨 기능은 동작하지 않음
 * - 서버/환경설정은 Constants.java 기반으로 관리
 */
package com.example.plog.autofill;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import com.example.plog.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.plog.util.Constants;

public class AutoFillFragment extends Fragment {

    private static final String TAG_KAKAO = "KAKAO_API";
    private static final String TAG_WEATHER = "OPENWEATHER_API";
    private static final OkHttpClient client = new OkHttpClient();

    private TextView tvDate, tvWeather, tvLocation;
    private ImageView ivSelectedPhoto;
    private EditText etTitle, etContent;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    // Step 1. 화면 생성 및 초기 설정
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_auto_fill, container, false);

        initViews(view);
        initImagePicker();
        initPermissionLauncher();
        setClickEvents();
        setDefaultAutoFillData();

        return view;
    }

    // Step 2. XML UI 요소 연결
    private void initViews(View view) {
        tvDate = view.findViewById(R.id.tvDate);
        tvWeather = view.findViewById(R.id.tvWeather);
        tvLocation = view.findViewById(R.id.tvLocation);
        ivSelectedPhoto = view.findViewById(R.id.ivSelectedPhoto);
        etTitle = view.findViewById(R.id.etTitle);
        etContent = view.findViewById(R.id.etContent);
    }

    // Step 3. 사진 선택 클릭 이벤트 등록
    private void setClickEvents() {
        ivSelectedPhoto.setOnClickListener(
                v -> checkPermissionAndShowPickerDialog()
        );
    }

    // Step 4. 초기 자동입력 기본값 표시
    private void setDefaultAutoFillData() {
        applyAutoFillData(
                new AutoFillData(
                        "사진을 선택해주세요",
                        "날씨 정보 없음",
                        "위치 정보 없음",
                        null
                )
        );
    }

    // Step 5. 사진 위치 권한 확인
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

    // Step 6. 권한 요청 결과 처리
    private void initPermissionLauncher() {
        permissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> showImageSelectDialog()
                );
    }

    // Step 7. 갤러리 / 파일 선택 다이얼로그 표시
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

    // Step 8. 갤러리 앱으로 이미지 선택
    private void openGalleryPicker() {
        Intent intent =
                new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                );

        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        imagePickerLauncher.launch(intent);
    }

    // Step 9. 파일 앱으로 이미지 선택
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

    // Step 10. 선택된 이미지 URI 수신
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

                            persistReadPermissionIfPossible(imageUri);
                            ivSelectedPhoto.setImageURI(imageUri);
                            readExifAndApplyToUi(imageUri);
                        }
                );
    }

    // Step 11. 파일 선택 URI 권한 유지
    private void persistReadPermissionIfPossible(Uri imageUri) {
        try {
            requireContext()
                    .getContentResolver()
                    .takePersistableUriPermission(
                            imageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
        } catch (SecurityException ignored) {
            // ACTION_PICK으로 선택한 갤러리 URI는 영구 권한 저장이 불가능할 수 있음
        }
    }

    // Step 12. EXIF 정보 추출 후 날짜/위치/날씨 자동입력 처리
    private void readExifAndApplyToUi(Uri imageUri) {
        try (
                InputStream inputStream =
                        requireContext()
                                .getContentResolver()
                                .openInputStream(imageUri)
        ) {
            if (inputStream == null) return;

            ExifInterface exif = new ExifInterface(inputStream);

            String displayDate =
                    formatExifDateTime(
                            getExifDateTime(exif)
                    );

            double[] latLong = exif.getLatLong();

            applyAutoFillData(
                    new AutoFillData(
                            displayDate,
                            "날씨 조회 중",
                            latLong == null ? "GPS 위치 정보 없음" : "주소 조회 중",
                            imageUri
                    )
            );

            if (latLong == null) return;

            double latitude = latLong[0];
            double longitude = latLong[1];

            getAddressFromKakao(latitude, longitude);
            getWeatherFromOpenWeather(latitude, longitude);

        } catch (IOException e) {
            Log.e("EXIF_ERROR", "EXIF 정보 읽기 실패", e);
        }
    }

    // Step 13. EXIF 촬영 날짜 추출
    private String getExifDateTime(ExifInterface exif) {
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

        return dateTime;
    }

    // Step 14. Kakao API로 위도/경도를 주소로 변환
    private void getAddressFromKakao(double latitude, double longitude) {
        if (Constants.KAKAO_REST_API_KEY.isEmpty()) {
            Log.e(TAG_KAKAO, "KAKAO_REST_API_KEY가 설정되지 않았습니다.");
            return;
        }

        String url =
                Constants.KAKAO_COORD_TO_ADDRESS_URL
                        + "?x=" + longitude
                        + "&y=" + latitude
                        + "&input_coord=WGS84";

        Request request =
                new Request.Builder()
                        .url(url)
                        .addHeader(
                                "Authorization",
                                "KakaoAK " + Constants.KAKAO_REST_API_KEY
                        )
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG_KAKAO, "주소 API 요청 실패", e);
            }

            @Override
            public void onResponse(
                    @NonNull Call call,
                    @NonNull Response response
            ) throws IOException {
                if (response.body() == null) return;

                String body = response.body().string();

                try {
                    JSONObject json = new JSONObject(body);
                    JSONArray docs = json.getJSONArray("documents");

                    if (docs.length() == 0) {
                        updateLocationText("주소 정보 없음");
                        return;
                    }

                    JSONObject first = docs.getJSONObject(0);
                    JSONObject addressObj = first.getJSONObject("address");

                    String region1 = addressObj.getString("region_1depth_name");
                    String region2 = addressObj.getString("region_2depth_name");
                    String region3 = addressObj.getString("region_3depth_name");

                    String address = region1 + " " + region2 + " " + region3;

                    Log.d(TAG_KAKAO, "주소: " + address);
                    updateLocationText(address);

                } catch (Exception e) {
                    Log.e(TAG_KAKAO, "주소 JSON 파싱 오류", e);
                }
            }
        });
    }

    // Step 15. OpenWeather API로 현재 날씨 조회
    private void getWeatherFromOpenWeather(double latitude, double longitude) {
        if (Constants.OPENWEATHER_API_KEY.isEmpty()) {
            Log.e(TAG_WEATHER, "OPENWEATHER_API_KEY가 설정되지 않았습니다.");
            return;
        }

        String url =
                Constants.OPENWEATHER_CURRENT_WEATHER_URL
                        + "?lat=" + latitude
                        + "&lon=" + longitude
                        + "&units=metric"
                        + "&lang=kr"
                        + "&appid=" + Constants.OPENWEATHER_API_KEY;

        Request request =
                new Request.Builder()
                        .url(url)
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG_WEATHER, "날씨 API 요청 실패", e);
            }

            @Override
            public void onResponse(
                    @NonNull Call call,
                    @NonNull Response response
            ) throws IOException {
                if (response.body() == null) return;

                String body = response.body().string();

                try {
                    JSONObject json = new JSONObject(body);

                    JSONObject main = json.getJSONObject("main");
                    double temp = main.getDouble("temp");

                    JSONArray weatherArray = json.getJSONArray("weather");
                    JSONObject weatherObj = weatherArray.getJSONObject(0);

                    String weatherMain = weatherObj.getString("main");
                    String weatherText =
                            convertWeatherToKorean(weatherMain, temp);

                    Log.d(TAG_WEATHER, "날씨: " + weatherText);
                    updateWeatherText(weatherText);

                } catch (Exception e) {
                    Log.e(TAG_WEATHER, "날씨 JSON 파싱 오류", e);
                }
            }
        });
    }

    // Step 16. OpenWeather 날씨 코드를 한국어 표시용 문자열로 변환
    private String convertWeatherToKorean(String weatherMain, double temp) {
        String weatherIcon;
        String weatherName;

        switch (weatherMain) {
            case "Clear":
                weatherIcon = "☀️";
                weatherName = "맑음";
                break;

            case "Clouds":
                weatherIcon = "☁️";
                weatherName = "흐림";
                break;

            case "Rain":
                weatherIcon = "🌧️";
                weatherName = "비";
                break;

            case "Drizzle":
                weatherIcon = "🌦️";
                weatherName = "이슬비";
                break;

            case "Thunderstorm":
                weatherIcon = "⛈️";
                weatherName = "천둥번개";
                break;

            case "Snow":
                weatherIcon = "❄️";
                weatherName = "눈";
                break;

            case "Mist":
            case "Fog":
            case "Haze":
            case "Smoke":
            case "Dust":
            case "Sand":
            case "Ash":
                weatherIcon = "🌫️";
                weatherName = "안개/먼지";
                break;

            case "Squall":
            case "Tornado":
                weatherIcon = "🌪️";
                weatherName = "강풍";
                break;

            default:
                weatherIcon = "🌍";
                weatherName = "날씨 정보";
                break;
        }

        return weatherIcon + " " + weatherName + " " + String.format("%.0f", temp) + "℃";
    }

    // Step 17. EXIF 날짜 형식 변환
    private String formatExifDateTime(String exifDateTime) {
        if (exifDateTime == null) {
            return "촬영 날짜 정보 없음";
        }

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

    // Step 18. 자동입력 정보 전체 반영
    private void applyAutoFillData(AutoFillData data) {
        if (data == null) return;

        tvDate.setText("📅 " + data.date);
        tvWeather.setText("☀️ " + data.weather);
        tvLocation.setText("📍 " + data.location);

        if (data.imageUri != null) {
            ivSelectedPhoto.setImageURI(data.imageUri);
        }
    }

    // Step 19. 위치 TextView만 갱신
    private void updateLocationText(String location) {
        requireActivity().runOnUiThread(
                () -> tvLocation.setText("📍 " + location)
        );
    }

    // Step 20. 날씨 TextView만 갱신
    private void updateWeatherText(String weather) {
        requireActivity().runOnUiThread(
                () -> tvWeather.setText(weather)
        );
    }

    // Step 21. 자동 추출 데이터 저장용 내부 클래스
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