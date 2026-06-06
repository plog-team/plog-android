package com.example.plog.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * DB 스키마 PhotoLabel 테이블에 대응
 * - photo_id + label_id 복합 PK
 * - confidence만 보유 (라벨 텍스트는 Label 테이블 JOIN)
 */
@Entity(
        tableName = "photo_label",
        primaryKeys = { "photo_id", "label_id" },          // 복합 PK
        foreignKeys = {
                @ForeignKey(
                        entity    = PhotoEntity.class,
                        parentColumns = "id",
                        childColumns  = "photo_id",
                        onDelete  = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity    = LabelEntity.class,
                        parentColumns = "id",
                        childColumns  = "label_id",
                        onDelete  = ForeignKey.CASCADE
                )
        },
        indices = { @Index("photo_id"), @Index("label_id") }
)
public class PhotoLabelEntity {

    @ColumnInfo(name = "photo_id")
    public int photoId;

    @ColumnInfo(name = "label_id")
    public int labelId;

    @ColumnInfo(name = "confidence")
    public float confidence;         // 0.0 ~ 1.0
}
