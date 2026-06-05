package com.example.plog.data.db.entity;
// data/db/entity/LabelEntity.java
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * DB 스키마 Label 테이블에 대응
 * - 라벨 이름만 보관, confidence는 PhotoLabel에 있음
 * - name은 UNIQUE 제약
 */
@Entity(
        tableName = "label",
        indices = { @Index(value = "name", unique = true) }
)
public class LabelEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;              // "cat", "food", "sky" 등
}
