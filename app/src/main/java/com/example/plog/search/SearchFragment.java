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

// 검색어 변경 감지를 위한 import
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;

public class SearchFragment extends Fragment {

    private RecyclerView rvSearchDiary;
    private SearchDiaryAdapter adapter;
    private ArrayList<SearchDiary> diaryList;
    private EditText etSearch;
    private ImageButton btnSearch;

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

        // 검색창 XML id 연결
        etSearch = view.findViewById(R.id.et_search);

        // 검색 버튼 연결
                btnSearch = view.findViewById(R.id.btnSearch);

        // 돋보기 버튼 클릭 시 검색 실행
        btnSearch.setOnClickListener(v -> {

            String keyword =
                    etSearch.getText()
                            .toString()
                            .trim();

            loadDiariesFromServer(keyword);

        });

        // 키보드의 검색 버튼을 눌렀을 때 서버 검색 실행
        etSearch.setOnEditorActionListener((v, actionId, event) -> {

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                String keyword = etSearch.getText().toString().trim();

                loadDiariesFromServer(keyword);

                return true;
            }

            return false;
        });

        // 검색창의 글자가 변경될 때 실행
        // 검색어를 전부 지우면 다시 전체 일기 목록을 불러온다.
        etSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 입력 전에는 별도 처리 없음
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String keyword = s.toString().trim();

                // 검색창이 비어 있으면 전체 목록 다시 조회
                if (keyword.isEmpty()) {
                    loadDiariesFromServer();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 입력 후에는 별도 처리 없음
            }
        });



        return view;
    }

    //전체 일기목록 조회 함수
    //검색어 없이 호출하면 서버에서 전체데이터 가져옴
    private void loadDiariesFromServer(){
        loadDiariesFromServer("");
    }


    // SpringBoot 서버에 요청해서 MySQL 일기 데이터를 가져오는 함수
    private void loadDiariesFromServer(String keyword) {

        // HTTP 통신을 위한 OkHttp 클라이언트 생성
        OkHttpClient client = new OkHttpClient();

        // Android 에뮬레이터에서 내 컴퓨터 localhost로 접근하는 주소(ip주소입력)
        //노트북/폰 연결에 따라 주소 변경필요
        String baseUrl = "http://10.127.7.137:8080/api/diaries/search";
        //String baseUrl = "http://10.0.2.2:8080/api/diaries/search";

        String url;

        //검색어 없을시 전체목록조회
        if(keyword==null || keyword.isEmpty()) {
            url = baseUrl;
        }
        else{
            url = baseUrl+"?keyword=" +keyword;
        }

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