package com.example.plog.data.db.dao;
// LabelDao.java
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.plog.data.db.entity.LabelEntity;
// data/db/dao/LabelDao.java
@Dao
public interface LabelDao {

    /**
     * 라벨 이름으로 삽입 또는 기존 id 반환 (upsert 패턴)
     * IGNORE: 이미 있으면 -1 반환 → 이후 getByName()으로 id 조회
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(LabelEntity entity);

    @Query("SELECT * FROM label WHERE name = :name LIMIT 1")
    LabelEntity getByName(String name);

    /**
     * 라벨 name으로 id를 가져오거나 없으면 INSERT — Repository에서 처리
     */
    @Query("SELECT id FROM label WHERE name = :name LIMIT 1")
    int getIdByName(String name);
}
