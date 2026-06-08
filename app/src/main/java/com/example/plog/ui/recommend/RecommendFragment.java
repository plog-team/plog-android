package com.example.plog.ui.recommend;
import androidx.core.widget.NestedScrollView;
import android.Manifest;
import android.app.AlertDialog;
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
import com.example.plog.api.model.*;
import com.example.plog.network.ApiClient;
import com.example.plog.ui.recommend.adapter.*;
import com.example.plog.ui.recommend.model.PlaceItem;
import java.util.*;
import retrofit2.*;
import com.example.plog.model.ApiResponse;


public class RecommendFragment extends Fragment {

    private static final int LOCATION_REQUEST = 1001;
    private static final int PAGE_SIZE        = 10;
    private static final int NEARBY_RADIUS    = 2000;
    private static final int FEATURED_RADIUS  = 1000;

    private NestedScrollView layoutContent;
    private View layoutLoading, layoutError, layoutEmpty, layoutFeaturedSection;
    private RecyclerView rvFeatured, rvNearby;
    private TextView tvNearbyCount, tvSortBtn;
    private TextView tvTabAll, tvTabTourist, tvTabCulture, tvTabEvent, tvTabFood;

    private FeaturedAdapter featuredAdapter;
    private RecommendAdapter recommendAdapter;
    private final List<PlaceItem> featuredList = new ArrayList<>();
    private final List<PlaceItem> originalList = new ArrayList<>();
    private final List<PlaceItem> nearbyList   = new ArrayList<>();

    // 클릭로그 기반 선호 카테고리
    private List<String> preferredTypeIds = new ArrayList<>();

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
        layoutContent         = v.findViewById(R.id.layoutContent);
        layoutLoading         = v.findViewById(R.id.layoutLoading);
        layoutError           = v.findViewById(R.id.layoutError);
        layoutEmpty           = v.findViewById(R.id.layoutEmpty);
        layoutFeaturedSection = v.findViewById(R.id.layoutFeaturedSection);
        rvFeatured            = v.findViewById(R.id.rvFeatured);
        rvNearby              = v.findViewById(R.id.rvNearby);
        tvNearbyCount         = v.findViewById(R.id.tvNearbyCount);
        tvSortBtn             = v.findViewById(R.id.tvSortBtn);
        tvTabAll              = v.findViewById(R.id.tvTabAll);
        tvTabTourist          = v.findViewById(R.id.tvTabTourist);
        tvTabCulture          = v.findViewById(R.id.tvTabCulture);
        tvTabEvent            = v.findViewById(R.id.tvTabEvent);
        tvTabFood             = v.findViewById(R.id.tvTabFood);
        v.findViewById(R.id.btnRetry).setOnClickListener(x -> getLocationAndLoad());
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

    private void setupSortButton() {
        tvSortBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), tvSortBtn);
            if (isSortedByPopularity) {
                popup.getMenu().add(0, 1, 0, "인기순 ✓");
                popup.getMenu().add(0, 2, 1, "거리순");
            } else {
                popup.getMenu().add(0, 2, 0, "거리순 ✓");
                popup.getMenu().add(0, 1, 1, "인기순");
            }
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    if (!isSortedByPopularity) {
                        isSortedByPopularity = true;
                        tvSortBtn.setText("인기순 ∨");
                        if (!originalList.isEmpty()) fetchCongestionAndSort();
                    }
                } else {
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
        selected.setTextColor(requireContext().getColor(android.R.color.white));
        isSortedByPopularity = false;
        tvSortBtn.setText("거리순 ∨");
        currentPage = 1;
        hasMorePages = true;
        originalList.clear();
        nearbyList.clear();
        featuredList.clear();
        featuredAdapter.updateItems(featuredList);

        // 전체 탭이면 취향 추천 섹션 표시, 다른 탭이면 숨김
        layoutFeaturedSection.setVisibility(
                typeId.isEmpty() ? View.VISIBLE : View.GONE);

        loadPlaces();
    }

    private void getLocationAndLoad() {
        fusedLocationClient = LocationServices
                .getFusedLocationProviderClient(requireActivity());

        // 권한 없으면 요청
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST);
            return;
        }

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
                    } else {
                        showLocationFallbackDialog();
                    }
                    loadPlaces();
                })
                .addOnFailureListener(e ->
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(loc -> {
                                    if (loc != null) {
                                        currentLat = loc.getLatitude();
                                        currentLon = loc.getLongitude();
                                    } else {
                                        showLocationFallbackDialog();
                                    }
                                    loadPlaces();
                                })
                                .addOnFailureListener(e2 -> {
                                    showLocationFallbackDialog();
                                    loadPlaces();
                                }));
    }

    // 위치 실패 팝업 (폴백은 유지)
    private void showLocationFallbackDialog() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("위치를 가져올 수 없어요")
                        .setMessage("현재 위치를 확인할 수 없어\n서울 기본 위치 기준으로 장소를 보여드립니다.")
                        .setPositiveButton("확인", null)
                        .show());
    }

    // 선호 카테고리 로드 후 장소 조회
    private void loadPlaces() {
        currentPage = 1;
        hasMorePages = true;
        showLoading();

        if (selectedTypeId.isEmpty()) {
            ApiClient.getApiService().getPreference()
                    .enqueue(new Callback<PreferenceResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<PreferenceResponse> call,
                                               @NonNull Response<PreferenceResponse> resp) {
                            if (!isAdded()) return;
                            if (resp.isSuccessful() && resp.body() != null
                                    && resp.body().data != null
                                    && resp.body().data.preferredCategories != null) {
                                preferredTypeIds = resp.body().data.preferredCategories;
                            } else {
                                preferredTypeIds = new ArrayList<>();
                            }
                            fetchFeatured();
                            fetchPage(currentPage, true);
                        }

                        @Override
                        public void onFailure(@NonNull Call<PreferenceResponse> call,
                                              @NonNull Throwable t) {
                            preferredTypeIds = new ArrayList<>();
                            fetchFeatured();
                            fetchPage(currentPage, true);
                        }
                    });
        } else {
            fetchPage(currentPage, true);
        }
    }

    // 취향 기반 featured 전용 호출
    private void fetchFeatured() {
        String featuredTypeId = preferredTypeIds.isEmpty() ? "" : preferredTypeIds.get(0);

        ApiClient.getApiService().getNearby(
                        currentLon, currentLat,
                        FEATURED_RADIUS, 100, 1, featuredTypeId)
                .enqueue(new Callback<ApiResponse<List<PlaceItemDto>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<PlaceItemDto>>> call,
                                           @NonNull Response<ApiResponse<List<PlaceItemDto>>> resp) {
                        if (!isAdded() || !resp.isSuccessful()
                                || resp.body() == null || resp.body().data == null) return;
                        List<PlaceItem> items = parseDtoItems(resp.body().data);
                        requireActivity().runOnUiThread(() -> {
                            if (!selectedTypeId.isEmpty()) return;
                            featuredList.clear();
                            featuredList.addAll(items);
                            featuredAdapter.updateItems(featuredList);
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<PlaceItemDto>>> call,
                                          @NonNull Throwable t) {}
                });
    }

    private void loadMorePlaces() {
        if (isLoadingMore || !hasMorePages) return;
        isLoadingMore = true;
        currentPage++;
        fetchPage(currentPage, false);
    }

    private void fetchPage(int page, boolean isFirst) {
        ApiClient.getApiService().getNearby(
                        currentLon, currentLat,
                        NEARBY_RADIUS, PAGE_SIZE, page, selectedTypeId)
                .enqueue(new Callback<ApiResponse<List<PlaceItemDto>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<PlaceItemDto>>> call,
                                           @NonNull Response<ApiResponse<List<PlaceItemDto>>> resp) {
                        if (!isAdded()) return;
                        isLoadingMore = false;

                        if (resp.isSuccessful() && resp.body() != null
                                && resp.body().data != null) {
                            List<PlaceItemDto> body = resp.body().data;

                            if (isFirst && body.isEmpty()) {
                                RecommendFragment.this.showEmpty();
                                return;
                            }

                            List<PlaceItem> newItems = parseDtoItems(body);

                            if (newItems.isEmpty()) {
                                hasMorePages = false;
                                if (isFirst) RecommendFragment.this.showEmpty();
                                return;
                            }

                            if (body.size() < PAGE_SIZE) hasMorePages = false;

                            if (isFirst) {
                                originalList.clear();
                                nearbyList.clear();
                            }
                            originalList.addAll(newItems);

                            requireActivity().runOnUiThread(() -> {
                                nearbyList.addAll(newItems);
                                recommendAdapter.updateItems(nearbyList);
                                tvNearbyCount.setText(nearbyList.size() + "곳");
                                showContent();
                            });
                        } else {
                            if (isFirst) RecommendFragment.this.showError();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<PlaceItemDto>>> call,
                                          @NonNull Throwable t) {
                        isLoadingMore = false;
                        if (isAdded() && isFirst) RecommendFragment.this.showError();
                    }
                });
    }

    private void fetchCongestionAndSort() {
        if (originalList.isEmpty()) return;

        Map<String, List<PlaceItem>> areaMap = new HashMap<>();
        for (PlaceItem item : originalList) {
            String area = matchAreaFromAddress(item.getAddress(), item.getTitle());
            if (area == null) area = getNearestSeoulArea(item.getLatitude(), item.getLongitude());
            if (area != null) {
                if (!areaMap.containsKey(area)) areaMap.put(area, new ArrayList<>());
                areaMap.get(area).add(item);
            }
        }

        if (areaMap.isEmpty()) {
            // 서울 외 지역 → 거리순 유지 + 안내
            requireActivity().runOnUiThread(() -> {
                isSortedByPopularity = false;
                tvSortBtn.setText("거리순 ∨");
                new AlertDialog.Builder(requireContext())
                        .setTitle("인기순 정렬 불가")
                        .setMessage("주변에 실시간 혼잡도 지원 지역이 없어\n거리순으로 유지합니다.")
                        .setPositiveButton("확인", null)
                        .show();
            });
            return;
        }

        int[] completed    = {0};
        int[] successCount = {0};
        int total = areaMap.size();

        for (String area : areaMap.keySet()) {
            List<PlaceItem> itemsInArea = areaMap.get(area);
            String encodedArea;
            try { encodedArea = java.net.URLEncoder.encode(area, "UTF-8"); }
            catch (Exception e) { encodedArea = area; }

            ApiClient.getApiService().getCongestion(encodedArea)
                    .enqueue(new Callback<CongestionDto>() {
                        @Override
                        public void onResponse(@NonNull Call<CongestionDto> call,
                                               @NonNull Response<CongestionDto> resp) {
                            if (resp.isSuccessful() && resp.body() != null
                                    && !"정보없음".equals(resp.body().congestionLevel)) {
                                int score = congestionToScore(resp.body().congestionLevel);
                                for (PlaceItem item : itemsInArea) {
                                    item.setCongestionScore(score);
                                }
                                synchronized (completed) { successCount[0]++; }
                            }
                            checkAndSort(completed, total, successCount);
                        }

                        @Override
                        public void onFailure(@NonNull Call<CongestionDto> call,
                                              @NonNull Throwable t) {
                            checkAndSort(completed, total, successCount);
                        }
                    });
        }
    }

    private void checkAndSort(int[] completed, int total, int[] successCount) {
        synchronized (completed) {
            completed[0]++;
            if (completed[0] >= total) {
                // 성공한 권역이 하나도 없으면 안내 + 거리순 유지
                if (successCount[0] == 0) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            isSortedByPopularity = false;
                            tvSortBtn.setText("거리순 ∨");
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("인기순 정렬 실패")
                                    .setMessage("혼잡도 정보를 가져오지 못했어요.\n거리순으로 유지합니다.")
                                    .setPositiveButton("확인", null)
                                    .show();
                        });
                    }
                    return;
                }
                List<PlaceItem> sorted = new ArrayList<>(originalList);
                Collections.sort(sorted,
                        (a, b) -> b.getCongestionScore() - a.getCongestionScore());
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

    // DTO → PlaceItem 변환
    private List<PlaceItem> parseDtoItems(List<PlaceItemDto> dtos) {
        List<PlaceItem> result = new ArrayList<>();
        for (PlaceItemDto d : dtos) {
            result.add(new PlaceItem(
                    d.contentId, d.title, d.address,
                    d.imageUrl, getCategoryName(d.contentTypeId),
                    d.contentTypeId, d.dist, d.lat, d.lng));
        }
        return result;
    }

    // 클릭 시 로그 저장 후 상세 화면 이동
    private void navigateToDetail(PlaceItem item) {
        ApiClient.getApiService().saveClickLog(
                        new ClickLogRequest(
                                item.getContentId(),
                                item.getContentTypeId(),
                                item.getCategory()))
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> resp) {}
                    @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
                });

        Bundle b = new Bundle();
        b.putString("contentId",     item.getContentId());
        b.putString("contentTypeId", item.getContentTypeId());
        b.putString("title",         item.getTitle());
        b.putString("address",       item.getAddress());
        b.putString("imageUrl",      item.getImageUrl());
        b.putString("category",      item.getCategory());
        b.putString("distance",      item.getDistance());
        b.putString("congestion",    item.getCongestion());
        b.putFloat("latitude",       (float) item.getLatitude());
        b.putFloat("longitude",      (float) item.getLongitude());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_recommend_to_detail, b);
    }

    private String matchAreaFromAddress(String address, String title) {
        if (address == null && title == null) return null;
        String text = (address != null ? address : "") + (title != null ? title : "");
        if (text.contains("강남") || text.contains("삼성") || text.contains("코엑스")) return "강남 MICE 관광특구";
        if (text.contains("동대문") || text.contains("DDP"))                           return "동대문 관광특구";
        if (text.contains("명동") || text.contains("충무로"))                          return "명동 관광특구";
        if (text.contains("홍대") || text.contains("홍익") || text.contains("합정"))   return "홍대 관광특구";
        if (text.contains("이태원") || text.contains("한남"))                         return "이태원 관광특구";
        if (text.contains("종로") || text.contains("청계"))                           return "종로·청계 관광특구";
        if (text.contains("잠실") || text.contains("송파"))                           return "잠실 관광특구";
        if (text.contains("인사동") || text.contains("익선"))                         return "인사동·익선동";
        if (text.contains("북촌") || text.contains("삼청"))                           return "북촌한옥마을";
        if (text.contains("경복궁") || text.contains("광화문"))                       return "경복궁";
        if (text.contains("창덕궁") || text.contains("종묘"))                         return "창덕궁·종묘";
        if (text.contains("남산"))                                                    return "남산공원";
        if (text.contains("서울숲") || text.contains("성수"))                         return "서울숲";
        if (text.contains("여의도"))                                                  return "여의도";
        if (text.contains("영등포") || text.contains("타임스퀘어"))                   return "영등포·타임스퀘어";
        if (text.contains("신촌") || text.contains("이대") || text.contains("이화여대")) return "신촌·이대";
        if (text.contains("왕십리") || text.contains("뚝섬"))                         return "왕십리·성수·뚝섬";
        if (text.contains("노원") || text.contains("불암산"))                         return "노원·불암산";
        if (text.contains("수유") || text.contains("도봉"))                           return "수유리·도봉산";
        if (text.contains("어린이대공원") || text.contains("광진"))                   return "어린이대공원";
        return null;
    }

    private String getNearestSeoulArea(double lat, double lon) {
        if (lat < 37.4 || lat > 37.7 || lon < 126.7 || lon > 127.2) return null;
        double[][] areas = {
                {37.5636, 126.9869}, {37.5563, 126.9236}, {37.5172, 127.0473},
                {37.5796, 126.9770}, {37.5340, 126.9940}, {37.5133, 127.1001},
                {37.5714, 126.9882}, {37.5824, 126.9830}, {37.5648, 127.0816},
                {37.5443, 127.0557}, {37.5219, 126.9245}, {37.5572, 126.9368},
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
            double d = Math.pow(lat - areas[i][0], 2) + Math.pow(lon - areas[i][1], 2);
            if (d < minDist) { minDist = d; nearest = names[i]; }
        }
        return nearest;
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

    private int congestionToScore(String level) {
        if (level == null) return 0;
        switch (level) {
            case "매우붐빔": return 3;
            case "붐빔":     return 2;
            case "보통":     return 1;
            default:         return 0;
        }
    }

    private String getCategoryName(String typeId) {
        if (typeId == null) return "기타";
        switch (typeId) {
            case "12": return "관광지";   case "14": return "문화시설";
            case "15": return "행사일정"; case "25": return "여행코스";
            case "28": return "레포츠";   case "32": return "숙박";
            case "38": return "쇼핑";     case "39": return "음식점";
            default:   return "기타";
        }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return 0.0; }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndLoad();
        } else {
            // 권한 거부 → 팝업 + 기본 폴백
            if (isAdded()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("위치 권한이 없어요")
                        .setMessage("위치 권한을 허용하지 않아\n서울 기본 위치 기준으로 장소를 보여드립니다.")
                        .setPositiveButton("확인", (d, w) -> loadPlaces())
                        .show();
            } else {
                loadPlaces();
            }
        }
    }
}