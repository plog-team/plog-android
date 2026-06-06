// data/repository/AnalysisRepository.java
package com.example.plog.data.repository;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.plog.data.db.AppDatabase;
import com.example.plog.data.db.dao.PhotoLabelDao;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.data.db.dao.PhotoDao;
import com.example.plog.data.db.dao.UserPreferenceScoreDao;
import com.example.plog.data.db.entity.PhotoLocationEntity;
import com.example.plog.data.db.entity.UserPreferenceScoreEntity;
import com.example.plog.model.MonthlyReport;
import com.example.plog.model.MonthlyReport.ActivityRadius;
import com.example.plog.model.MonthlyReport.LabelItem;
import com.example.plog.model.MonthlyReport.PlaceItem;
import com.example.plog.model.PreferenceUpdateRequest;
import com.example.plog.network.ApiClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisRepository {

    private static final float MIN_CONFIDENCE = 0.65f;

    private final PhotoLocationDao        locationDao;
    private final PhotoLabelDao           photoLabelDao;
    private final PhotoDao                photoDao;
    private final UserPreferenceScoreDao  preferenceDao;

    public AnalysisRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        locationDao   = db.photoLocationDao();
        photoLabelDao = db.photoLabelDao();
        photoDao      = db.photoDao();
        preferenceDao = db.userPreferenceScoreDao();
    }

    @WorkerThread
    @NonNull
    public MonthlyReport buildReport(int userId, int year, int month) {
        long[] range = getMonthRange(year, month);
        long startMs = range[0];
        long endMs   = range[1];

        List<PhotoLocationEntity> allLocations =
                locationDao.getLocationsInRangeSync(userId, startMs, endMs);

        List<PhotoLocationDao.LocationCluster> clusters =
                locationDao.getTopLocationClusters(userId, startMs, endMs);

        List<PhotoLabelDao.LabelFrequency> labelFreqs =
                photoLabelDao.getTopLabelsByMonth(userId, startMs, endMs, MIN_CONFIDENCE);

        // ← PhotoDao로 변경
        int totalCount   = photoDao.getTotalPhotoCount(userId, startMs, endMs);
        int activeDays   = photoDao.getActiveDayCount(userId, startMs, endMs);
        int uniqueLabels = photoLabelDao.getUniqueLabelCount(
                userId, startMs, endMs, MIN_CONFIDENCE);

        List<MonthlyReport.PlaceItem> topPlaces   = buildPlaceItems(clusters);
        List<MonthlyReport.LabelItem> topLabels   = buildLabelItems(labelFreqs);
        MonthlyReport.ActivityRadius  radius       = calculateActivityRadius(allLocations);

        savePreferenceScores(userId, topLabels);
        String                        summaryText  = generateSummary(year, month,
                totalCount, activeDays,
                topPlaces, topLabels, radius);

        return new MonthlyReport.Builder()
                .year(year).month(month)
                .totalPhotoCount(totalCount)
                .activeDayCount(activeDays)
                .uniqueLabelCount(uniqueLabels)
                .topPlaces(topPlaces)
                .topLabels(topLabels)
                .activityRadius(radius)
                .summaryText(summaryText)
                .build();
    }

    // 나머지 메서드 동일

    // EMA 가중치 — 이번 달 70%, 누적 30%
    private static final float EMA_ALPHA  = 0.7f;
    // 새 달 진입 시 기존 점수에 적용하는 decay 계수
    private static final float EMA_DECAY  = 0.7f;

    // ── top3 라벨 → user_preference_score EMA 저장 ───────────────────────
    private void savePreferenceScores(int userId,
                                      @NonNull List<MonthlyReport.LabelItem> labels) {
        long now = System.currentTimeMillis();

        // 마지막 저장 시각 확인 — 새 달인지 판단
        Long lastUpdated = preferenceDao.getLastUpdated(userId);
        boolean isNewMonth = lastUpdated == null || !isSameMonth(lastUpdated, now);

        if (isNewMonth) {
            // 새 달: 기존 점수 전체 decay (TOP3에서 빠진 카테고리도 자연 감소)
            preferenceDao.decayAll(userId, EMA_DECAY);
        }

        // 기존 점수 Map으로 로드 (EMA 계산용)
        Map<String, Float> existingScores = new HashMap<>();
        for (UserPreferenceScoreEntity e : preferenceDao.getByUser(userId)) {
            existingScores.put(e.category, e.score);
        }

        List<String> top3Categories = new ArrayList<>();
        int top = Math.min(labels.size(), 3);
        for (int i = 0; i < top; i++) {
            MonthlyReport.LabelItem label = labels.get(i);
            float oldScore = existingScores.getOrDefault(label.labelText, 0f);
            // 새 달: EMA 블렌딩 / 같은 달: 현재 분석 결과로 업데이트
            float newScore = isNewMonth
                    ? EMA_ALPHA * label.avgConfidence + (1 - EMA_ALPHA) * oldScore
                    : label.avgConfidence;

            UserPreferenceScoreEntity e = new UserPreferenceScoreEntity();
            e.userId    = userId;
            e.category  = label.labelText;
            e.score     = newScore;
            e.updatedAt = now;
            preferenceDao.upsert(e);

            top3Categories.add(label.labelText);
        }

        // 서버 동기화 — fire-and-forget (실패해도 리포트 표시에 영향 없음)
        syncPreferencesToServer(top3Categories);
    }

    private void syncPreferencesToServer(@NonNull List<String> categories) {
        if (categories.isEmpty()) return;
        try {
            ApiClient.getApiService()
                    .updatePreferences(new PreferenceUpdateRequest(categories))
                    .execute();
            Log.d("AnalysisRepository", "선호도 서버 동기화 완료: " + categories);
        } catch (Exception e) {
            Log.w("AnalysisRepository", "선호도 서버 동기화 실패 (무시): " + e.getMessage());
        }
    }

    private static boolean isSameMonth(long ts1, long ts2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(ts1);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(ts2);
        return c1.get(Calendar.YEAR)  == c2.get(Calendar.YEAR)
            && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
    }

    // ── LocationCluster → PlaceItem 변환 ──────────────────────────────────
    @NonNull
    private List<PlaceItem> buildPlaceItems(
            @NonNull List<PhotoLocationDao.LocationCluster> clusters) {
        List<PlaceItem> items = new ArrayList<>();
        int rank = 1;
        for (PhotoLocationDao.LocationCluster c : clusters) {
            String locationName = (c.locationName != null && !c.locationName.isEmpty())
                    ? c.locationName : null;
            items.add(new PlaceItem(
                    c.clusterLat, c.clusterLng,
                    c.visitCount, c.firstVisit, c.lastVisit,
                    rank + "위 장소",
                    locationName
            ));
            rank++;
        }
        return items;
    }

    // ── LabelFrequency → LabelItem 변환 ───────────────────────────────────
    @NonNull
    private List<LabelItem> buildLabelItems(
            @NonNull List<PhotoLabelDao.LabelFrequency> freqs) {
        List<LabelItem> items = new ArrayList<>();
        for (PhotoLabelDao.LabelFrequency f : freqs) {
            items.add(new LabelItem(f.labelText, f.frequency, f.avgConfidence));
        }
        return items;
    }

    // ── 활동 반경 계산 ─────────────────────────────────────────────────────
    // Step1. 무게중심 = 위도/경도 평균
    // Step2. 최대 반경 = 중심에서 가장 먼 좌표까지 거리
    // Step3. 총 이동 거리 = 인접 좌표 간 거리 합
    @androidx.annotation.Nullable
    private ActivityRadius calculateActivityRadius(
            @NonNull List<PhotoLocationEntity> locations) {

        if (locations.isEmpty()) {
            return null;
        }

        // Step1 — 무게중심
        double sumLat = 0, sumLng = 0;
        for (PhotoLocationEntity e : locations) {
            sumLat += e.latitude;
            sumLng += e.longitude;
        }
        double centerLat = sumLat / locations.size();
        double centerLng = sumLng / locations.size();

        float[] dist = new float[1];

        // Step2 — 최대 반경
        double maxRadius = 0;
        for (PhotoLocationEntity e : locations) {
            Location.distanceBetween(
                    centerLat, centerLng,
                    e.latitude, e.longitude,
                    dist
            );
            if (dist[0] > maxRadius) maxRadius = dist[0];
        }

        // Step3 — 총 이동 거리
        double totalDist = 0;
        for (int i = 0; i < locations.size() - 1; i++) {
            Location.distanceBetween(
                    locations.get(i).latitude,     locations.get(i).longitude,
                    locations.get(i + 1).latitude, locations.get(i + 1).longitude,
                    dist
            );
            totalDist += dist[0];
        }

        return new ActivityRadius(centerLat, centerLng, maxRadius, totalDist);
    }

    // ── 요약 문장 자동 생성 (템플릿 기반) ────────────────────────────────────
    @NonNull
    private String generateSummary(int year, int month,
                                   int totalCount, int activeDays,
                                   @NonNull List<PlaceItem> places,
                                   @NonNull List<LabelItem> labels,
                                   @androidx.annotation.Nullable ActivityRadius radius) {
        StringBuilder sb = new StringBuilder();
        sb.append(year).append("년 ").append(month).append("월 리포트\n\n");
        sb.append(activeDays).append("일 동안 총 ")
                .append(totalCount).append("장의 사진을 기록했어요.\n");

        if (!places.isEmpty()) {
            PlaceItem top = places.get(0);
            String placeName = (top.locationName != null) ? top.locationName : top.placeLabel;
            sb.append("사진을 가장 많이 찍은 곳은 ").append(placeName)
                    .append("으로, ").append(top.visitCount).append("장을 기록했어요.\n");
        }

        if (!labels.isEmpty()) {
            LabelItem top = labels.get(0);
            sb.append("자주 등장한 키워드는 '").append(top.labelText)
                    .append("'(").append(top.frequency).append("회)이에요.\n");
        }

        if (radius != null && radius.radiusMeters > 0) {
            sb.append("활동 반경 약 ").append(radius.getRadiusText())
                    .append(", 총 이동 ").append(radius.getTotalDistanceText()).append("이에요.");
        }

        return sb.toString();
    }

    // ── 월 시작/종료 epoch ms ──────────────────────────────────────────────
    @NonNull
    private long[] getMonthRange(int year, int month) {
        Calendar start = Calendar.getInstance();
        start.set(year, month - 1, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(year, month - 1,
                start.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        end.set(Calendar.MILLISECOND, 999);

        return new long[]{ start.getTimeInMillis(), end.getTimeInMillis() };
    }
}