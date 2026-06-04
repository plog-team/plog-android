package com.example.plog.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.plog.data.db.entity.UserPreferenceScoreEntity;

import java.util.List;

@Dao
public interface UserPreferenceScoreDao {

    // user_id + category 유니크 인덱스 덕에 REPLACE가 upsert로 동작
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserPreferenceScoreEntity entity);

    @Query("SELECT * FROM user_preference_score WHERE user_id = :userId ORDER BY score DESC")
    List<UserPreferenceScoreEntity> getByUser(int userId);

    // 마지막 저장 시각 — 월 중복 저장 방지용
    @Query("SELECT MAX(updated_at) FROM user_preference_score WHERE user_id = :userId")
    Long getLastUpdated(int userId);

    // 새 달 진입 시 기존 점수 전체 decay
    @Query("UPDATE user_preference_score SET score = score * :factor WHERE user_id = :userId")
    void decayAll(int userId, float factor);
}
