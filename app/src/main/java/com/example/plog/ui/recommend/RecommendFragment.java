package com.example.plog.ui.recommend;
import androidx.core.widget.NestedScrollView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.*;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.*;
import com.google.android.gms.location.Priority;
import com.example.plog.R;
import com.example.plog.api.*;
import com.example.plog.api.model.*;
import com.example.plog.ui.recommend.adapter.*;
import com.example.plog.ui.recommend.model.PlaceItem;
import java.util.*;
import retrofit2.*;

public class RecommendFragment extends Fragment {

    private static final int LOCATION_REQUEST = 1001;
    private static final int PAGE_SIZE = 5;

    private NestedScrollView layoutContent;
    private View layoutLoading, layoutError, layoutEmpty;
    private RecyclerView rvFeatured, rvNearby;
    private TextView tvNearbyCount, tvSortBtn;
    private TextView tvTabAll, tvTabTourist, tvTabCulture, tvTabEvent, tvTabFood;

    private FeaturedAdapter featuredAdapter;
    private RecommendAdapter recommendAdapter;
    private final List<PlaceItem> featuredList = new ArrayList<>();
    private final List<PlaceItem> originalList = new ArrayList<>();
    private final List<PlaceItem> nearbyList   = new ArrayList<>();

    private double currentLat = 37.5665;
    private double currentLon = 126.9780;
    private String selectedTypeId = "";
    private boolean isSortedByPopularity = false;
    private boolean isLoadingMore = false;
    private int currentPage = 1;
    private boolean hasMorePages = true;
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupAdapters();
        setupCategoryTabs();
        setupSortButton();
        setupScrollPagination();
        getLocationAndLoad();
    }

    private void initViews(View v) {
        layoutContent = v.findViewById(R.id.layoutContent);
        layoutLoading = v.findViewById(R.id.layoutLoading);
        layoutError   = v.findViewById(R.id.layoutError);
        layoutEmpty   = v.findViewById(R.id.layoutEmpty);
        rvFeatured    = v.findViewById(R.id.rvFeatured);
        rvNearby      = v.findViewById(R.id.rvNearby);
        tvNearbyCount = v.findViewById(R.id.tvNearbyCount);
        tvSortBtn     = v.findViewById(R.id.tvSortBtn);
        tvTabAll      = v.findViewById(R.id.tvTabAll);
        tvTabTourist  = v.findViewById(R.id.tvTabTourist);
        tvTabCulture  = v.findViewById(R.id.tvTabCulture);
        tvTabEvent    = v.findViewById(R.id.tvTabEvent);
        tvTabFood     = v.findViewById(R.id.tvTabFood);
        v.findViewById(R.id.btnRetry)
                .setOnClickListener(x -> getLocationAndLoad());
    }

    private void setupAdapters() {
        FeaturedAdapter.OnItemClickListener click = this::navigateToDetail;
        featuredAdapter = new FeaturedAdapter(featuredList, click);
        rvFeatured.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFeatured.setAdapter(featuredAdapter);

        recommendAdapter = new RecommendAdapter(nearbyList, click);
        rvNearby.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNearby.setAdapter(recommendAdapter);
    }

    // 스크롤 페이지네이션
    private void setupScrollPagination() {
        layoutContent.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    View child = layoutContent.getChildAt(0);
                    if (child == null) return;
                    int diff = child.getBottom()
                            - (layoutContent.getHeight() + scrollY);
                    if (diff <= 300 && !isLoadingMore && hasMorePages) {
                        loadMorePlaces();
                    }
                });
    }

    // 정렬 드롭다운
    private void setupSortButton() {
        tvSortBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), tvSortBtn);

            // 현재 정렬 기준이 위에 오도록
            if (isSortedByPopularity) {
                popup.getMenu().add(0, 1, 0, "인기순 ✓");
                popup.getMenu().add(0, 2, 1, "거리순");
            } else {
                popup.getMenu().add(0, 2, 0, "거리순 ✓");
                popup.getMenu().add(0, 1, 1, "인기순");
            }

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    // 인기순 선택
                    if (!isSortedByPopularity) {
                        isSortedByPopularity = true;
                        tvSortBtn.setText("인기순 ∨");
                        if (!originalList.isEmpty()) {
                            fetchCongestionAndSort();
                        }
                    }
                } else {
                    // 거리순 선택
                    if (isSortedByPopularity) {
                        isSortedByPopularity = false;
                        tvSortBtn.setText("거리순 ∨");
                        nearbyList.clear();
                        nearbyList.addAll(originalList);
                        recommendAdapter.updateItems(nearbyList);
                    }
                }
                return true;
            });
            popup.show();
        });
    }

    private void setupCategoryTabs() {
        tvTabAll.setOnClickListener(v     -> selectTab(tvTabAll,     ""));
        tvTabTourist.setOnClickListener(v -> selectTab(tvTabTourist, "12"));
        tvTabCulture.setOnClickListener(v -> selectTab(tvTabCulture, "14"));
        tvTabEvent.setOnClickListener(v   -> selectTab(tvTabEvent,   "15"));
        tvTabFood.setOnClickListener(v    -> selectTab(tvTabFood,    "39"));
        selectTab(tvTabAll, "");
    }

    private void selectTab(TextView selected, String typeId) {
        selectedTypeId = typeId;
        for (TextView tab : new TextView[]{tvTabAll, tvTabTourist,
                tvTabCulture, tvTabEvent, tvTabFood}) {
            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
            tab.setTextColor(requireContext().getColor(R.color.gray_400));
        }
        selected.setBackgroundResource(R.drawable.bg_tab_selected);
        selected.setTextColor(
                requireContext().getColor(android.R.color.white));
        isSortedByPopularity = false;
        tvSortBtn.setText("거리순 ∨");
        currentPage = 1;
        hasMorePages = true;
        originalList.clear();
        nearbyList.clear();
        loadPlaces();
    }

    private void getLocationAndLoad() {
        fusedLocationClient = LocationServices
                .getFusedLocationProviderClient(requireActivity());
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST);
            return;
        }

        // getLastLocation은 에뮬레이터에서 null을 반환하는 경우가 많으므로
        // getCurrentLocation으로 실시간 위치를 요청한다
        com.google.android.gms.location.CurrentLocationRequest locationRequest =
                new com.google.android.gms.location.CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .setMaxUpdateAgeMillis(30000)
                        .setDurationMillis(5000)
                        .build();

        fusedLocationClient.getCurrentLocation(locationRequest, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        currentLat = loc.getLatitude();
                        currentLon = loc.getLongitude();
                    }
                    loadPlaces();
                })
                .addOnFailureListener(e -> {
                    // 실패 시 getLastLocation으로 fallback
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(loc -> {
                                if (loc != null) {
                                    currentLat = loc.getLatitude();
                                    currentLon = loc.getLongitude();
                                }
                                loadPlaces();
                            })
                            .addOnFailureListener(e2 -> loadPlaces());
                });
    }

    // 첫 페이지 로드
    private void loadPlaces() {
        currentPage = 1;
        hasMorePages = true;
        showLoading();
        fetchPage(currentPage, true);
    }

    // 추가 페이지 로드
    private void loadMorePlaces() {
        if (isLoadingMore || !hasMorePages) return;
        isLoadingMore = true;
        currentPage++;
        fetchPage(currentPage, false);
    }

    private void fetchPage(int page, boolean isFirst) {
        TourApiClient.getTourService().getNearby(
                        TourApiClient.TOUR_API_KEY,
                        currentLon, currentLat,
                        2000, "Plog", "AND", "json",
                        PAGE_SIZE, page, selectedTypeId)
                .enqueue(new Callback<TourResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TourResponse> call,
                                           @NonNull Response<TourResponse> resp) {
                        if (!isAdded()) return;
                        isLoadingMore = false;

                        if (resp.isSuccessful() && resp.body() != null) {
                            TourResponse.Body body = resp.body().response.body;

                            if (isFirst && (body.totalCount == 0
                                    || body.items == null)) {
                                showEmpty();
                                return;
                            }

                            if (body.items == null || body.items.item == null
                                    || body.items.item.isEmpty()) {
                                hasMorePages = false;
                                return;
                            }

                            List<PlaceItem> newItems =
                                    parseItems(body.items.item);

                            // 전체 페이지 수 초과 체크
                            int totalPages = (int) Math.ceil(
                                    (double) body.totalCount / PAGE_SIZE);
                            if (page >= totalPages) hasMorePages = false;

                            if (isFirst) {
                                originalList.clear();
                                nearbyList.clear();
                                featuredList.clear();
                            }

                            originalList.addAll(newItems);

                            requireActivity().runOnUiThread(() -> {
                                if (isFirst) {
                                    featuredList.addAll(originalList.subList(
                                            0, Math.min(3, originalList.size())));
                                    featuredAdapter.updateItems(featuredList);
                                }
                                nearbyList.addAll(newItems);
                                recommendAdapter.updateItems(nearbyList);
                                tvNearbyCount.setText(nearbyList.size() + "곳");
                                showContent();
                            });
                        } else {
                            if (isFirst) showError();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TourResponse> call,
                                          @NonNull Throwable t) {
                        isLoadingMore = false;
                        if (isAdded() && isFirst) showError();
                    }
                });
    }

    // 인기순 정렬
    // 서울시 권역 목록
    private String matchAreaFromAddress(String address, String title) {
        if (address == null && title == null) return null;
        String text = (address != null ? address : "")
                + (title != null ? title : "");

        if (text.contains("강남") || text.contains("삼성")
                || text.contains("코엑스"))        return "강남 MICE 관광특구";
        if (text.contains("동대문") || text.contains("DDP"))
            return "동대문 관광특구";
        if (text.contains("명동") || text.contains("충무로"))
            return "명동 관광특구";
        if (text.contains("홍대") || text.contains("홍익")
                || text.contains("합정"))          return "홍대 관광특구";
        if (text.contains("이태원") || text.contains("한남"))
            return "이태원 관광특구";
        if (text.contains("종로") || text.contains("청계"))
            return "종로·청계 관광특구";
        if (text.contains("잠실") || text.contains("송파"))
            return "잠실 관광특구";
        if (text.contains("인사동") || text.contains("익선"))
            return "인사동·익선동";
        if (text.contains("북촌") || text.contains("삼청"))
            return "북촌한옥마을";
        if (text.contains("경복궁") || text.contains("광화문"))
            return "경복궁";
        if (text.contains("창덕궁") || text.contains("종묘"))
            return "창덕궁·종묘";
        if (text.contains("남산"))                 return "남산공원";
        if (text.contains("서울숲") || text.contains("성수"))
            return "서울숲";
        if (text.contains("여의도"))               return "여의도";
        if (text.contains("영등포") || text.contains("타임스퀘어"))
            return "영등포·타임스퀘어";
        if (text.contains("신촌") || text.contains("이대")
                || text.contains("이화여대"))      return "신촌·이대";
        if (text.contains("왕십리") || text.contains("뚝섬"))
            return "왕십리·성수·뚝섬";
        if (text.contains("노원") || text.contains("불암산"))
            return "노원·불암산";
        if (text.contains("수유") || text.contains("도봉"))
            return "수유리·도봉산";
        if (text.contains("어린이대공원")
                || text.contains("광진"))          return "어린이대공원";
        return null; // 매칭 안 되면 score 0
    }

    private void fetchCongestionAndSort() {
        if (originalList.isEmpty()) return;

        // 각 장소마다 권역 매칭
        Map<String, List<PlaceItem>> areaMap = new HashMap<>();
        for (PlaceItem item : originalList) {
            String area = matchAreaFromAddress(
                    item.getAddress(), item.getTitle());
            if (area != null) {
                if (!areaMap.containsKey(area)) {
                    areaMap.put(area, new ArrayList<>());
                }
                areaMap.get(area).add(item);
            }
        }

        if (areaMap.isEmpty()) {
            // 서울 권역 매칭 없음 → 좌표 기반 가장 가까운 권역으로 fallback
            for (PlaceItem item : originalList) {
                String area = getNearestSeoulArea(
                        item.getLatitude(), item.getLongitude());
                if (area != null) {
                    if (!areaMap.containsKey(area)) areaMap.put(area, new ArrayList<>());
                    areaMap.get(area).add(item);
                }
            }
        }

        if (areaMap.isEmpty()) {
            // 그래도 매칭 없으면 (서울 외 지역) 정렬 불가 안내
            Toast.makeText(requireContext(),
                    "주변에 실시간 혼잡도 지원 지역이 없어요",
                    Toast.LENGTH_SHORT).show();
            isSortedByPopularity = false;
            tvSortBtn.setText("거리순 ∨");
            return;
        }

        // 매칭된 권역마다 서울시 API 호출
        int[] completed = {0};
        int total = areaMap.size();

        for (String area : areaMap.keySet()) {
            List<PlaceItem> itemsInArea = areaMap.get(area);
            String encodedArea;
            try {
                encodedArea = java.net.URLEncoder.encode(area, "UTF-8");
            } catch (Exception e) {
                encodedArea = area;
            }
            TourApiClient.getSeoulService()
                    .getRealtimeData(TourApiClient.SEOUL_API_KEY, encodedArea)
                    .enqueue(new Callback<SeoulResponse>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<SeoulResponse> call,
                                @NonNull Response<SeoulResponse> resp) {
                            if (resp.isSuccessful()
                                    && resp.body() != null
                                    && resp.body().CITYDATA != null
                                    && resp.body().CITYDATA.LIVE_PPLTN_STTS != null
                                    && !resp.body().CITYDATA
                                    .LIVE_PPLTN_STTS.isEmpty()) {
                                int score = congestionToScore(
                                        resp.body().CITYDATA
                                                .LIVE_PPLTN_STTS.get(0)
                                                .AREA_CONGEST_LVL);
                                // 해당 권역 장소들에만 점수 부여
                                for (PlaceItem item : itemsInArea) {
                                    item.setCongestionScore(score);
                                }
                            }
                            checkAndSort(completed, total);
                        }
                        @Override
                        public void onFailure(
                                @NonNull Call<SeoulResponse> call,
                                @NonNull Throwable t) {
                            checkAndSort(completed, total);
                        }
                    });
        }
    }

    // 위도/경도로 가장 가까운 서울 권역 반환
    private String getNearestSeoulArea(double lat, double lon) {
        // 서울 범위 밖이면 null
        if (lat < 37.4 || lat > 37.7 || lon < 126.7 || lon > 127.2) return null;

        // 주요 권역 좌표 매핑
        double[][] areas = {
                {37.5636, 126.9869}, // 명동 관광특구
                {37.5563, 126.9236}, // 홍대 관광특구
                {37.5172, 127.0473}, // 강남 MICE 관광특구
                {37.5796, 126.9770}, // 경복궁
                {37.5340, 126.9940}, // 이태원 관광특구
                {37.5133, 127.1001}, // 잠실 관광특구
                {37.5714, 126.9882}, // 인사동·익선동
                {37.5824, 126.9830}, // 북촌 한옥마을
                {37.5648, 127.0816}, // 어린이대공원
                {37.5443, 127.0557}, // 왕십리·성수·뚝섬
                {37.5219, 126.9245}, // 여의도
                {37.5572, 126.9368}, // 신촌·이대
        };
        String[] names = {
                "명동 관광특구", "홍대 관광특구", "강남 MICE 관광특구",
                "경복궁", "이태원 관광특구", "잠실 관광특구",
                "인사동·익선동", "북촌한옥마을", "어린이대공원",
                "왕십리·성수·뚝섬", "여의도", "신촌·이대"
        };

        double minDist = Double.MAX_VALUE;
        String nearest = null;
        for (int i = 0; i < areas.length; i++) {
            double d = Math.pow(lat - areas[i][0], 2)
                    + Math.pow(lon - areas[i][1], 2);
            if (d < minDist) {
                minDist = d;
                nearest = names[i];
            }
        }
        return nearest;
    }

    private String congestionToLabel(int score) {
        switch (score) {
            case 4: return "매우붐빔";
            case 3: return "붐빔";
            case 2: return "보통";
            case 1: return "여유";
            default: return "정보없음";
        }
    }

    private List<PlaceItem> parseItems(List<TourItem> raw) {
        List<PlaceItem> result = new ArrayList<>();
        for (TourItem t : raw) {
            result.add(new PlaceItem(t.contentid, t.title, t.addr1,
                    t.firstimage, getCategoryName(t.contenttypeid),
                    t.contenttypeid, t.dist,
                    parseDouble(t.mapy), parseDouble(t.mapx)));
        }
        return result;
    }

    private void navigateToDetail(PlaceItem item) {
        Bundle b = new Bundle();
        b.putString("contentId",     item.getContentId());
        b.putString("contentTypeId", item.getContentTypeId()); // 타입별 상세 필드 수신용
        b.putString("title",         item.getTitle());
        b.putString("address",       item.getAddress());
        b.putString("imageUrl",      item.getImageUrl());
        b.putString("category",      item.getCategory());
        b.putString("distance",      item.getDistance());
        b.putString("congestion",    item.getCongestion()); // 추가
        b.putFloat("latitude",       (float) item.getLatitude());
        b.putFloat("longitude",      (float) item.getLongitude());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_recommend_to_detail, b);
    }

    private void showLoading() {
        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }
    private void showContent() {
        layoutContent.setVisibility(View.VISIBLE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }
    private void showError() {
        requireActivity().runOnUiThread(() -> {
            layoutContent.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.GONE);
            layoutError.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        });
    }
    private void showEmpty() {
        requireActivity().runOnUiThread(() -> {
            layoutContent.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        });
    }

    private void checkAndSort(int[] completed, int total) {
        synchronized (completed) {
            completed[0]++;
            if (completed[0] >= total) {
                List<PlaceItem> sorted = new ArrayList<>(originalList);
                Collections.sort(sorted,
                        (a, b) -> b.getCongestionScore()
                                - a.getCongestionScore());
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        nearbyList.clear();
                        nearbyList.addAll(sorted);
                        recommendAdapter.updateItems(nearbyList);
                    });
                }
            }
        }
    }

    private int congestionToScore(String level) {
        if (level == null) return 0;
        switch (level) {
            case "매우붐빔": return 4;
            case "붐빔":     return 3;
            case "보통":     return 2;
            case "여유":     return 1;
            default:         return 0;
        }
    }
    private String getCategoryName(String typeId) {
        if (typeId == null) return "기타";
        switch (typeId) {
            case "12": return "관광지";  case "14": return "문화시설";
            case "15": return "행사일정"; case "25": return "여행코스";
            case "28": return "레포츠";  case "32": return "숙박";
            case "38": return "쇼핑";    case "39": return "음식점";
            default:   return "기타";
        }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return 0.0; }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndLoad();
        } else { loadPlaces(); }
    }
}