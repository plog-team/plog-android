package com.example.plog.autofill;


import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.plog.R;

/*
    자동 메타데이터 입력 화면 Fragment
    - XML 화면(fragment_auto_fill.xml)과 연결
    - 날짜 / 날씨 / 장소 데이터를 UI에 출력
    - 나중에 EXIF, GPS, 날씨 API 결과를 받아 화면에 반영
*/
public class AutoFillFragment extends Fragment {
    private TextView tvDate; // 날짜 표시 TextView

    private TextView tvWeather; // 날씨 표시 TextView

    private TextView tvLocation; // 장소 표시 TextView

    private ImageView ivSelectedPhoto;  // 선택된 이미지 표시 ImageView


    private EditText etTitle;  // 제목 입력창


    private EditText etContent; // 내용 입력창


    /*
        Fragment 화면 생성 시 가장 먼저 호출되는 부분
        fragment_auto_fill.xml 과 연결됨
    */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        // XML 화면 연결
        View view = inflater.inflate(
                R.layout.fragment_auto_fill,
                container,
                false
        );

        // XML 내부 View들 초기화
        initViews(view);


        //테스트용 데이터
        AutoFillData testData = new AutoFillData(
                "26/05/01(금)",   // 날짜
                "맑음",            // 날씨
                "가천대학교",      // 장소
                null              // 이미지 URI
        );

        // 화면에 데이터 반영
        applyAutoFillData(testData);

        return view;
    }


    /*
        XML id와 Java 변수 연결

        예:
        R.id.tvDate
        ↓
        fragment_auto_fill.xml 안의
        android:id="@+id/tvDate"
    */
    private void initViews(View view) {

        tvDate = view.findViewById(R.id.tvDate);

        tvWeather = view.findViewById(R.id.tvWeather);

        tvLocation = view.findViewById(R.id.tvLocation);

        ivSelectedPhoto = view.findViewById(R.id.ivSelectedPhoto);

        etTitle = view.findViewById(R.id.etTitle);

        etContent = view.findViewById(R.id.etContent);
    }


    /*
        실제 데이터를 화면(UI)에 출력하는 함수

        여기서:
        - 날짜
        - 날씨
        - 장소
        - 이미지

        를 XML 화면에 넣음
    */
    private void applyAutoFillData(AutoFillData data) {

        // 데이터 없으면 종료
        if (data == null) return;


        tvDate.setText("📅  " + data.date); //날짜출력

        tvWeather.setText("☀️  " + data.weather); //날씨출력

        tvLocation.setText("📍 " + data.location); //장소출력


        if (data.imageUri != null) {
            ivSelectedPhoto.setImageURI(data.imageUri); //이미지 존재시, 표시
        }
    }


    /*
        자동 입력 데이터를 담는 클래스

        역할:
        메타데이터 결과를 한 번에 저장

        예:
        date
        weather
        location
        imageUri
    */
    private static class AutoFillData {


        String date; // 날짜

        String weather; // 날씨

        String location; // 장소

        Uri imageUri; // 이미지 URI


        /*
            생성자
            객체 생성 시 값 저장
        */
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