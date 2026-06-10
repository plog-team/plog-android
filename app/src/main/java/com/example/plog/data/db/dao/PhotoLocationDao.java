package com.example.plog.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.plog.data.db.entity.PhotoLocationEntity;

import java.util.List;

@Dao
public interface PhotoLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PhotoLocationEntity entity);

    @Query("SELECT COUNT(*) FROM photo_location WHERE photo_id = :photoId")
    int countByPhotoId(int photoId);

    @Query("SELECT * FROM photo_location WHERE photo_id = :photoId")
    LiveData<PhotoLocationEntity> getByPhotoId(int photoId);

    // 지도 화면용 LiveData
    @Query("SELECT pl.* FROM photo_location pl " +
            "INNER JOIN photo p ON pl.photo_id = p.id " +
            "WHERE p.user_id    = :userId " +
            "AND p.is_deleted   = 0 " +
            "AND pl.latitude    != 0.0 " +
            "AND pl.longitude   != 0.0 " +
            "ORDER BY pl.taken_at ASC")
    LiveData<List<PhotoLocationEntity>> getAllWithLocationLive(int userId);

    // 지도 화면 + 사진 포함 LiveData
    @Query("SELECT pl.id, pl.photo_id AS photoId, pl.taken_at AS takenAt, " +
            "pl.latitude, pl.longitude, pl.location_name AS locationName, " +
            "pl.address, p.image_url AS imageUrl " +
            "FROM photo_location pl " +
            "INNER JOIN photo p ON pl.photo_id = p.id " +
            "WHERE p.user_id    = :userId " +
            "AND p.is_deleted   = 0 " +
            "AND pl.latitude    != 0.0 " +
            "AND pl.longitude   != 0.0 " +
            "ORDER BY pl.taken_at ASC")
    LiveData<List<PhotoLocationWithImage>> getAllWithLocationAndImage(int userId);

    // 월간 GPS 전체 (활동 반경 + 이동 경로용) — 일기에 사진 추가일 기준
    @Query("SELECT pl.* FROM photo_location pl " +
            "INNER JOIN photo p ON pl.photo_id = p.id " +
            "WHERE p.user_id    = :userId " +
            "AND p.is_deleted   = 0 " +
            "AND p.created_at   BETWEEN :startMs AND :endMs " +
            "AND pl.latitude    != 0.0 " +
            "AND pl.longitude   != 0.0 " +
            "ORDER BY pl.taken_at ASC")
    List<PhotoLocationEntity> getLocationsInRangeSync(int userId,
                                                      long startMs,
                                                      long endMs);

    // 월간 사진 많이 찍은 장소 TOP3 — 일기에 사진 추가일 기준
    @Query("SELECT " +
            "  ROUND(pl.latitude,  2) AS clusterLat, " +
            "  ROUND(pl.longitude, 2) AS clusterLng, " +
            "  COUNT(*)               AS visitCount, " +
            "  MIN(pl.taken_at)       AS firstVisit, " +
            "  MAX(pl.taken_at)       AS lastVisit, " +
            "  MAX(pl.location_name)  AS locationName " +
            "FROM photo_location pl " +
            "INNER JOIN photo p ON pl.photo_id = p.id " +
            "WHERE p.user_id    = :userId " +
            "AND p.is_deleted   = 0 " +
            "AND p.created_at   BETWEEN :startMs AND :endMs " +
            "AND pl.latitude    != 0.0 " +
            "AND pl.longitude   != 0.0 " +
            "GROUP BY ROUND(pl.latitude, 2), ROUND(pl.longitude, 2) " +
            "ORDER BY visitCount DESC " +
            "LIMIT 3")
    List<LocationCluster> getTopLocationClusters(int userId,
                                                 long startMs,
                                                 long endMs);

    // 현재 월 지도 핀 — photo.created_at 기준 필터
    @Query("SELECT pl.id, pl.photo_id AS photoId, pl.taken_at AS takenAt, " +
            "pl.latitude, pl.longitude, pl.location_name AS locationName, " +
            "pl.address, p.image_url AS imageUrl " +
            "FROM photo_location pl " +
            "INNER JOIN photo p ON pl.photo_id = p.id " +
            "WHERE p.user_id    = :userId " +
            "AND p.is_deleted   = 0 " +
            "AND p.created_at   BETWEEN :startMs AND :endMs " +
            "AND pl.latitude    != 0.0 " +
            "AND pl.longitude   != 0.0 " +
            "ORDER BY pl.taken_at ASC")
    LiveData<List<PhotoLocationWithImage>> getMonthlyLocationsWithImage(int userId,
                                                                        long startMs,
                                                                        long endMs);

    // locationName 업데이트
    @Query("UPDATE photo_location " +
            "SET location_name = :locationName, address = :address " +
            "WHERE photo_id = :photoId")
    void updateLocationName(int photoId, String locationName, String address);

    // GPS 있으나 location_name 미입력 항목 — 역지오코딩 backfill용 (삭제된 사진 제외)
    @Query("SELECT pl.* FROM photo_location pl " +
            "INNER JOIN photo p ON pl.photo_id = p.id " +
            "WHERE p.is_deleted  = 0 " +
            "AND pl.latitude     != 0.0 " +
            "AND pl.longitude    != 0.0 " +
            "AND pl.location_name IS NULL")
    List<PhotoLocationEntity> getMissingLocationNames();

    // 알림 기능
    @Query("SELECT pl.* FROM photo_location pl " +
            "INNER JOIN photo p ON pl.photo_id = p.id " +
            "WHERE p.user_id = :userId " +
            "AND p.is_deleted = 0 " +
            "AND pl.latitude != 0.0 " +
            "AND pl.longitude != 0.0")
    List<PhotoLocationEntity> getAllWithLocationSync(int userId);

    // 알림 기능 - 사진 포함
    @Query("SELECT pl.id, pl.photo_id AS photoId, pl.taken_at AS takenAt, " +
            "pl.latitude, pl.longitude, pl.location_name AS locationName, " +
            "pl.address, p.image_url AS imageUrl, p.server_photo_id AS serverPhotoId " +
            "FROM photo_location pl " +
            "INNER JOIN photo p ON pl.photo_id = p.id " +
            "WHERE p.user_id = :userId " +
            "AND p.is_deleted = 0 " +
            "AND pl.latitude != 0.0 " +
            "AND pl.longitude != 0.0")
    List<PhotoLocationWithImage> getAllWithLocationAndImageSync(int userId);

    // 같은 photo_id이면 위치 1개라고 보기
    @Query("DELETE FROM photo_location WHERE photo_id = :photoId")
    void deleteByPhotoId(int photoId);

    // ── 결과 클래스 ───────────────────────────────────────────────────────
    class LocationCluster {
        public double clusterLat;
        public double clusterLng;
        public int    visitCount;
        public long   firstVisit;
        public long   lastVisit;
        public String locationName;
    }

    class PhotoLocationWithImage {
        public int    id;
        public int    photoId;
        public long   takenAt;
        public double latitude;
        public double longitude;
        public String locationName;
        public String address;
        public String imageUrl;
        public Long   serverPhotoId;
    }
}