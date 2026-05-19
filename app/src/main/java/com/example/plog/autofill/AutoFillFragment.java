package com.example.plog.autofill;

import android.Manifest;import android.app.Activity;import android.content.Intent;import android.content.pm.PackageManager;import android.net.Uri;import android.os.Build;import android.os.Bundle;import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;import android.view.View;import android.view.ViewGroup;import android.widget.EditText;import android.widget.ImageView;import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;import androidx.activity.result.contract.ActivityResultContracts;import androidx.annotation.NonNull;import androidx.annotation.Nullable;import androidx.appcompat.app.AlertDialog;import androidx.core.content.ContextCompat;import androidx.exifinterface.media.ExifInterface;import androidx.fragment.app.Fragment;

import com.example.plog.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;


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

            //이미지 EXIF 읽는 객체생성
            ExifInterface exif =
                    new ExifInterface(inputStream);

            // 촬영 날짜 읽기 없으면 일반날짜태그 읽기
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

            // GPS 위치 읽기(위도,경도)
            double[] latLong =
                    exif.getLatLong();

            if(latLong!=null){
                double latitude = latLong[0];
                double longitude = latLong[1];

                //카카오맵,OpenWeather API 호출
                getAddressFromKakao(latitude,longitude);
                getWeatherFromOpenWeather(latitude,longitude);

            }
            else {
                applyAutoFillData(
                        new AutoFillData(
                                displayDate,
                                "날씨 API연결전",
                                "GPS 위치정보 없음",
                                imageUri

                        )
                );
            }





            // 추출한 정보를 화면에 표시
            applyAutoFillData(
                    new AutoFillData(
                            displayDate,
                            "날씨 API 연결 전",
                            "GPS위치정보 없음",
                            imageUri
                    )
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //카카오 api요청(위도,경도 -> 주소)
    private void getAddressFromKakao(double latitude,double longitude){
        //OKHttp 클라이언트 생성
        OkHttpClient client = new OkHttpClient();

        //주소 요청양식
        String url = "https://dapi.kakao.com/v2/local/geo/coord2address.json"+"?x="+longitude+"&y="+latitude+"&input_coord=WGS84";

        Request request = new Request.Builder().url(url).addHeader("Authorization","KakaoAK cad55149d40de2258b65d10a8a3c3f58").build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e(
                        "KAKAO_API",
                        "API 요청 실패",
                        e
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    Log.d(
                            "KAKAO_API",
                            "응답 원본: " + body
                    );

//                    도시 큰 단위 3개까지 반환
                    JSONObject json = new JSONObject(body);
                    JSONArray docs = json.getJSONArray("documents");
                    JSONObject first = docs.getJSONObject(0);
                    JSONObject addressObj = first.getJSONObject("address");
                    String region1 = addressObj.getString("region_1depth_name");
                    String region2 = addressObj.getString("region_2depth_name");
                    String region3 = addressObj.getString("region_3depth_name");

                    String address =region1+" "+region2+" "+region3;

                    Log.d(
                            "KAKAO_API",
                            "주소: " + address
                    );
                    requireActivity().runOnUiThread(() ->
                    {
                        tvLocation.setText(address);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    // OpenWeather API 요청 함수
    private void getWeatherFromOpenWeather(double latitude,double longitude){
        //OKHttp 클라이언트 생성
        OkHttpClient client = new OkHttpClient();

        // OpenWeather API 요청 URL 생성
        String url = "https://api.openweathermap.org/data/2.5/weather"+"?lat="+latitude+"&lon="+longitude
                +"&units="+"metric"+"&lang=kr"+"&appid="+"009ff088b4325f66b2ab2ad8d94ec88f";

        Request request = new Request.Builder().url(url).build();

        // 비동기 요청 시작
        client.newCall(request).enqueue(new Callback() {
            // API 요청 실패 시 실행
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e(
                        "OpenWeather_API",
                        "날씨API 요청 실패",
                        e
                );
            }

            // API 응답 성공 시 실행
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 응답 body가 없으면 종료
                if(response.body()==null){return;}

                // JSON 응답 문자열 저장
                String body = response.body().string();

                try {
                    Log.d(
                            "OpenWeather_API",
                            "응답 원본: " + body
                    );

                    // JSON 문자열 → JSONObject 변환
                    // main:현재날씨정보객체, temp:현재온도, weather:날씨배열
                    JSONObject json = new JSONObject(body);
                    JSONObject main = json.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    JSONArray weatherArray = json.getJSONArray("weather");
                    JSONObject weatherObj=weatherArray.getJSONObject(0);

                    //OpenWeather의 큰 날씨 분류값
                    //ex) Clear, Clouds, Rain, Snow, Thunderstorm, Mist
                    String weatherMain = weatherObj.getString("main");

                    String weatherName;
                    String weatherIcon;

                    switch(weatherMain) {
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


                    // 최종 출력 문자열 생성
                    String weatherText = weatherIcon+" "+weatherName+" "+String.format("%.0f",temp)+"℃";

                    requireActivity().runOnUiThread(() ->
                    {
                        tvWeather.setText(weatherText);
                    });
                } catch (Exception e) {
                    Log.e(
                            // JSON 파싱 실패 시
                            "WEATHER_API",
                            "JSON 파싱 오류",
                            e
                    );
                    e.printStackTrace();
                }

            }
        });
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