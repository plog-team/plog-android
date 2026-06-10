/*
 * SearchFragment
 *
 * [기능]
 * - SpringBoot 서버에서 일기 목록 조회
 * - 키워드 기반 일기 검색 기능 제공
 * - RecyclerView를 이용한 검색 결과 출력
 *
 * [서버]
 * - SpringBoot + MySQL 연동
 * - BASE_URL은 Constants.java에서 관리
 *
 * [사용 기술]
 * - OkHttp
 * - RecyclerView
 * - JSON Parsing
 *
 * [외부 환경]
 * - 서버 실행 필요
 * - 인터넷 또는 동일 네트워크 연결 필요
 *
 * [주의]
 * - 서버 미연결 시 검색 기능은 동작하지 않음
 * - BASE_URL 환경에 따라 변경 가능
 */
package com.example.plog.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.util.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.widget.TextView;
import android.app.AlertDialog;

import com.google.android.material.datepicker.MaterialDatePicker;

import androidx.core.util.Pair;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
// 검색 화면 Fragment
public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";
    private static final OkHttpClient client = new OkHttpClient();

    private RecyclerView rvSearchDiary;
    private SearchDiaryAdapter adapter;
    private ArrayList<SearchDiary> diaryList;

    private EditText etSearch;
    private ImageButton btnSearch;
    private TextView tvSortLatest;
    private String currentSort = "latest";
    private ImageButton btnSort;

    private TextView btnDate;
    private TextView btnEmotion;
    private View filterChipScroll;
    private TextView chipDate;
    private TextView chipEmotion;

    private String selectedStartDate = "";
    private String selectedEndDate = "";
    private String selectedEmotion = "";

    // Step 1. 화면 생성 및 초기 설정
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initViews(view);
        initRecyclerView();
        initSearchEvents();
        initFilterEvents();

        loadDiariesFromServer();

        return view;
    }

    // Step 2. XML UI 요소 연결
    private void initViews(View view) {
        rvSearchDiary = view.findViewById(R.id.rvSearchDiary);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btnSearch);
        tvSortLatest = view.findViewById(R.id.tvSortLatest);

        btnDate = view.findViewById(R.id.btnDate);
        btnEmotion = view.findViewById(R.id.btnEmotion);
        filterChipScroll = view.findViewById(R.id.filterChipScroll);
        chipDate = view.findViewById(R.id.chipDate);
        chipEmotion = view.findViewById(R.id.chipEmotion);
    }

    // Step 3. RecyclerView 초기화
    private void initRecyclerView() {
        diaryList = new ArrayList<>();
        adapter = new SearchDiaryAdapter(diaryList);

        rvSearchDiary.setLayoutManager(
                new LinearLayoutManager(getContext())
        );

        rvSearchDiary.setAdapter(adapter);
    }

    // Step 4. 검색 버튼, 키보드 검색, 검색창 변경 이벤트 설정
    private void initSearchEvents() {
        btnSearch.setOnClickListener(
                v -> searchByKeyword()
        );
        tvSortLatest.setOnClickListener(v -> {
            if ("latest".equals(currentSort)) {
                currentSort = "oldest";
                tvSortLatest.setText("오래된순");
            } else {
                currentSort = "latest";
                tvSortLatest.setText("최신순");
            }

            searchByKeyword();
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchByKeyword();
                return true;
            }

            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
                // 입력 전 별도 처리 없음
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                String keyword = s.toString().trim();

                if (keyword.isEmpty()) {
                    loadDiariesFromServer();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 입력 후 별도 처리 없음
            }
        });
    }

    // 날짜/감정 필터 버튼 이벤트 설정
    private void initFilterEvents() {
        btnDate.setOnClickListener(v -> showDateRangePicker());
        btnEmotion.setOnClickListener(v -> showEmotionPicker());

        // 날짜 필터 칩 클릭 시 날짜 필터 해제
        chipDate.setOnClickListener(v -> {
            selectedStartDate = "";
            selectedEndDate = "";
            chipDate.setVisibility(View.GONE);
            updateFilterChipArea();
            searchByKeyword();
        });

        // 감정 필터 칩 클릭 시 감정 필터 해제
        chipEmotion.setOnClickListener(v -> {
            selectedEmotion = "";
            chipEmotion.setVisibility(View.GONE);
            updateFilterChipArea();
            searchByKeyword();
        });
    }

    // Step 5. 검색어 기반 서버 조회 실행
    private void searchByKeyword() {
        String keyword =
                etSearch.getText()
                        .toString()
                        .trim();

        loadDiariesFromServer(keyword);
    }

    // Step 6. 전체 일기 목록 조회
    private void loadDiariesFromServer() {
        loadDiariesFromServer("");
    }

    // Step 7. SpringBoot 서버에 일기 목록 검색 요청
    private void loadDiariesFromServer(String keyword) {
        if (Constants.BASE_URL == null || Constants.BASE_URL.isEmpty()) {
            Log.e(TAG, "BASE_URL이 설정되지 않았습니다.");
            return;
        }

        String url = buildSearchUrl(keyword);

        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("plog_prefs", android.content.Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        Request request =
                new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(
                    @NonNull Call call,
                    @NonNull IOException e
            ) {
                Log.e(TAG, "서버 연결 실패", e);
            }

            @Override
            public void onResponse(
                    @NonNull Call call,
                    @NonNull Response response
            ) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "서버 응답 실패: " + response.code());
                    return;
                }

                String json = response.body().string();
                Log.d(TAG, "서버 응답: " + json);

                parseDiariesAndUpdateUi(json);
            }
        });
    }

    // Step 8. 검색 요청 URL 생성
    private String buildSearchUrl(String keyword) {
        String baseUrl = Constants.BASE_URL + "api/diaries/search";

        try {
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append("?sort=").append(currentSort);

            if (keyword != null && !keyword.isEmpty()) {
                String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
                urlBuilder.append("&keyword=").append(encodedKeyword);
            }
            if (!selectedStartDate.isEmpty() && !selectedEndDate.isEmpty()) {
                urlBuilder.append("&startDate=")
                        .append(URLEncoder.encode(selectedStartDate, "UTF-8"));

                urlBuilder.append("&endDate=")
                        .append(URLEncoder.encode(selectedEndDate, "UTF-8"));
            }

            if (!selectedEmotion.isEmpty()) {
                urlBuilder.append("&emotion=")
                        .append(URLEncoder.encode(selectedEmotion, "UTF-8"));
            }
            return urlBuilder.toString();

        } catch (Exception e) {
            Log.e(TAG, "검색어 인코딩 실패", e);
            return baseUrl + "?sort=" + currentSort;
        }
    }

    // Step 9. 서버 응답 JSON 파싱 후 UI 갱신
    private void parseDiariesAndUpdateUi(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray array = root.getJSONArray("data");

            Log.d(TAG, "data 배열 개수: " + array.length());

            requireActivity().runOnUiThread(() -> {
                try {
                    diaryList.clear();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Log.d(TAG, "item " + i + ": " + obj.toString());

                        diaryList.add(
                                new SearchDiary(
                                        obj.optString("diary_date", obj.optString("date")),
                                        obj.optString("emotion", ""),
                                        obj.optString("title", ""),
                                        obj.optString("content", obj.optString("body")),
                                        obj.optString("location_name", obj.optString("location")),
                                        obj.optString("image_url", obj.optString("imageUrl"))
                                )
                        );
                    }

                    Log.d(TAG, "RecyclerView 갱신 개수: " + diaryList.size());
                    adapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e(TAG, "일기 목록 UI 갱신 실패", e);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "일기 목록 JSON 파싱 실패", e);
        }
    }

    // 날짜 범위 선택 캘린더 표시
    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("날짜 범위 선택")
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }

            selectedStartDate = formatServerDate(selection.first);
            selectedEndDate = formatServerDate(selection.second);

            String displayStartDate = formatDisplayDate(selection.first);
            String displayEndDate = formatDisplayDate(selection.second);

            chipDate.setText("날짜: " + displayStartDate + " ~ " + displayEndDate + "  ✕");
            chipDate.setVisibility(View.VISIBLE);

            updateFilterChipArea();
            searchByKeyword();
        });

        picker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");
    }

    // 감정 선택 다이얼로그 표시
    private void showEmotionPicker() {
        String[] emotions = {
                "기쁨",
                "슬픔",
                "신남",
                "화남",
                "평온",
                "설렘",
                "불안",
                "피곤"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("감정 선택")
                .setItems(emotions, (dialog, which) -> {
                    selectedEmotion = emotions[which];

                    chipEmotion.setText("감정: " + selectedEmotion + "  ✕");
                    chipEmotion.setVisibility(View.VISIBLE);

                    updateFilterChipArea();
                    searchByKeyword();

                    dialog.dismiss();
                })
                .show();
    }

    // 선택된 필터 칩 영역 표시 여부 갱신
    private void updateFilterChipArea() {
        boolean hasDateFilter = chipDate.getVisibility() == View.VISIBLE;
        boolean hasEmotionFilter = chipEmotion.getVisibility() == View.VISIBLE;

        if (hasDateFilter || hasEmotionFilter) {
            filterChipScroll.setVisibility(View.VISIBLE);
        } else {
            filterChipScroll.setVisibility(View.GONE);
        }
    }

    // 서버 전송용 날짜 형식: yyyy-MM-dd
    private String formatServerDate(long timeMillis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                .format(new Date(timeMillis));
    }

    // 화면 표시용 날짜 형식: yy/MM/dd
    private String formatDisplayDate(long timeMillis) {
        return new SimpleDateFormat("yy/MM/dd", Locale.KOREA)
                .format(new Date(timeMillis));
    }
}