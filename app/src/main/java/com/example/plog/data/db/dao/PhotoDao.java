package com.example.plog.data.db.dao;

// PhotoDao.java
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.plog.data.db.entity.PhotoEntity;
import java.util.List;
@Dao
public interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PhotoEntity entity);

    @Query("SELECT * FROM photo WHERE id = :id AND is_deleted = 0")
    LiveData<PhotoEntity> getById(int id);

    @Query("SELECT * FROM photo WHERE user_id = :userId AND is_deleted = 0")
    LiveData<List<PhotoEntity>> getByUser(int userId);

    @Query("UPDATE photo SET is_deleted = 1 WHERE id = :id")
    void softDelete(int id);

    @Query("UPDATE photo SET is_deleted = 1 WHERE image_url = :imageUrl")
    void softDeleteByImageUrl(String imageUrl);

    @Query("SELECT server_photo_id FROM photo WHERE image_url = :imageUrl AND is_deleted = 0")
    Long getServerPhotoIdByImageUrl(String imageUrl);

    @Query("UPDATE photo SET server_photo_id = :serverPhotoId WHERE image_url = :imageUrl")
    void updateServerPhotoId(String imageUrl, long serverPhotoId);

    // data/db/dao/PhotoDao.java 에 추가
    @Query("SELECT COUNT(DISTINCT " +
            "  STRFTIME('%Y-%m-%d', created_at / 1000, 'unixepoch', 'localtime')) " +
            "FROM photo " +
            "WHERE user_id    = :userId " +
            "AND created_at   BETWEEN :startMs AND :endMs " +
            "AND is_deleted   = 0")
    int getActiveDayCount(int userId, long startMs, long endMs);

    @Query("SELECT COUNT(*) FROM photo " +
            "WHERE user_id  = :userId " +
            "AND created_at BETWEEN :startMs AND :endMs " +
            "AND is_deleted = 0")
    int getTotalPhotoCount(int userId, long startMs, long endMs);

    @Query("SELECT * FROM photo WHERE server_photo_id = :serverPhotoId AND is_deleted = 0 LIMIT 1")
    PhotoEntity getByServerPhotoIdSync(long serverPhotoId);

    @Query("SELECT id FROM photo WHERE server_photo_id = :serverPhotoId AND is_deleted = 0 LIMIT 1")
    Integer getLocalIdByServerPhotoIdSync(long serverPhotoId);
}
