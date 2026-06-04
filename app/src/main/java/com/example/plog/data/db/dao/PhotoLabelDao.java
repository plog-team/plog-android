package com.example.plog.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.plog.data.db.entity.PhotoLabelEntity;

import java.util.List;

@Dao
public interface PhotoLabelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PhotoLabelEntity> entities);

    // 사진별 라벨 목록 (RecyclerView용 LiveData)
    @Query("SELECT l.name AS labelText, pl.confidence " +
            "FROM photo_label pl " +
            "INNER JOIN label l ON pl.label_id = l.id " +
            "WHERE pl.photo_id = :photoId " +
            "ORDER BY pl.confidence DESC")
    LiveData<List<PhotoLabelResult>> getLabelsByPhotoId(int photoId);

    // 월간 TOP3 라벨
    @Query("SELECT " +
            "  l.name             AS labelText, " +
            "  COUNT(*)           AS frequency, " +
            "  AVG(pl.confidence) AS avgConfidence, " +
            "  MAX(pl.confidence) AS maxConfidence " +
            "FROM photo_label pl " +
            "INNER JOIN label  l ON pl.label_id = l.id " +
            "INNER JOIN photo  p ON pl.photo_id = p.id " +
            "WHERE p.user_id      = :userId " +
            "AND p.created_at     BETWEEN :startMs AND :endMs " +
            "AND p.is_deleted     = 0 " +
            "AND pl.confidence    >= :minConfidence " +
            "GROUP BY l.name " +
            "ORDER BY frequency DESC " +
            "LIMIT 3")
    List<LabelFrequency> getTopLabelsByMonth(int userId,
                                             long startMs,
                                             long endMs,
                                             float minConfidence);

    // 고유 라벨 종류 수
    @Query("SELECT COUNT(DISTINCT l.name) " +
            "FROM photo_label pl " +
            "INNER JOIN label  l ON pl.label_id = l.id " +
            "INNER JOIN photo  p ON pl.photo_id = p.id " +
            "WHERE p.user_id   = :userId " +
            "AND p.created_at  BETWEEN :startMs AND :endMs " +
            "AND p.is_deleted  = 0 " +
            "AND pl.confidence >= :minConfidence")
    int getUniqueLabelCount(int userId, long startMs, long endMs,
                            float minConfidence);

    // ── 결과 클래스 ───────────────────────────────────────────────────────
    class PhotoLabelResult {
        public String labelText;
        public float  confidence;
    }

    class LabelFrequency {
        public String labelText;
        public int    frequency;
        public float  avgConfidence;
        public float  maxConfidence;
    }
}