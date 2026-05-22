package com.example.plog.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.plog.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchFragment extends Fragment {

    private RecyclerView rvSearchDiary;
    private SearchDiaryAdapter adapter;
    private ArrayList<SearchDiary> diaryList;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        // fragment_search.xml 화면을 Fragment에 연결
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // RecyclerView를 XML id와 연결
        rvSearchDiary = view.findViewById(R.id.rvSearchDiary);

        // 서버에서 받아온 일기 데이터를 담을 리스트 생성
        diaryList = new ArrayList<>();

        // RecyclerView에 데이터를 연결해줄 Adapter 생성
        adapter = new SearchDiaryAdapter(diaryList);

        // RecyclerView를 세로 목록 형태로 설정
        rvSearchDiary.setLayoutManager(new LinearLayoutManager(getContext()));

        // RecyclerView에 Adapter 연결
        rvSearchDiary.setAdapter(adapter);

        // SpringBoot 서버에서 일기 데이터 불러오기
        loadDiariesFromServer();

        return view;
    }


    // SpringBoot 서버에 요청해서 MySQL 일기 데이터를 가져오는 함수
    private void loadDiariesFromServer() {

        // HTTP 통신을 위한 OkHttp 클라이언트 생성
        OkHttpClient client = new OkHttpClient();

        // Android 에뮬레이터에서 내 컴퓨터 localhost로 접근하는 주소
        //노트북/폰 연결에 따라 주소 변경필요
        String url = "http://172.30.1.12:8080/api/diaries/search";
        //String url = "http://10.0.2.2:8080/api/diaries/search";

        // GET 방식으로 서버에 요청 생성
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        // 서버 요청을 비동기로 실행
        client.newCall(request).enqueue(new Callback() {

            // 서버 연결 실패 시 실행
            @Override
            public void onFailure(
                    @NonNull Call call,
                    @NonNull IOException e
            ) {

                // [추가] 서버 연결 실패 원인 로그 출력
                android.util.Log.e(
                        "SearchFragment",
                        "서버 연결 실패",
                        e
                );

                e.printStackTrace();
            }

            // 서버 응답 성공 시 실행
            @Override
            public void onResponse(
                    @NonNull Call call,
                    @NonNull Response response
            ) throws IOException {

                // 응답이 실패 상태면 함수 종료
                if (!response.isSuccessful()) {
                    return;
                }

                // 서버에서 받은 JSON 문자열 저장
                String json = response.body().string();
                android.util.Log.d(
                        "SearchFragment",
                        "서버 응답: " + json
                );

                try {
                    // JSON 배열 형태로 변환
                    JSONArray array = new JSONArray(json);

                    // UI 변경은 반드시 메인 스레드에서 실행
                    requireActivity().runOnUiThread(() -> {

                        try {
                            // 기존 리스트 비우기
                            diaryList.clear();

                            // JSON 배열을 하나씩 꺼내 SearchDiary 객체로 변환
                            for (int i = 0; i < array.length(); i++) {

                                JSONObject obj = array.getJSONObject(i);

                                diaryList.add(new SearchDiary(
                                        obj.getString("diary_date"),
                                        obj.getString("emotion"),
                                        obj.getString("title"),
                                        obj.getString("content"),
                                        obj.getString("location_name"),
                                        ""
                                ));
                            }

                            // 데이터 변경을 Adapter에 알려서 화면 갱신
                            adapter.notifyDataSetChanged();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}