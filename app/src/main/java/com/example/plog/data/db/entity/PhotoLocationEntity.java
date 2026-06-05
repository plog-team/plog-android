package com.example.plog.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * DB 스키마 PhotoLocation 테이블에 대응
 * - taken_at, latitude, longitude, location_name, address
 * - cameraModel 컬럼은 스키마에 없으므로 제거
 */
@Entity(
        tableName = "photo_location",
        foreignKeys = @ForeignKey(
                entity    = PhotoEntity.class,
                parentColumns = "id",
                childColumns  = "photo_id",
                onDelete  = ForeignKey.CASCADE
        ),
        indices = { @Index("photo_id") }
)
public class PhotoLocationEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "photo_id")
    public int photoId;

    @ColumnInfo(name = "taken_at")
    public long takenAt;             // EXIF 촬영 시각 (epoch ms)

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "location_name")
    public String locationName;      // 역지오코딩 결과 (예: "가천대학교")

    @ColumnInfo(name = "address")
    public String address;           // 상세 주소
}
