package com.example.plog.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/*@Entity(
        tableName = "photo",
        foreignKeys = @ForeignKey(
                entity    = UserEntity.class,
                parentColumns = "id",
                childColumns  = "user_id",
                onDelete  = ForeignKey.CASCADE
        ),
        indices = { @Index("user_id") }
)

 */
// data/db/entity/PhotoEntity.java
// foreignKeys 제거 - 확인용
@Entity(
        tableName = "photo",
        indices = { @Index("user_id") }
)
public class PhotoEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public int userId;

    @ColumnInfo(name = "image_url")
    public String imageUrl;          // ✅ imageUri → image_url

    @ColumnInfo(name = "file_size")
    public long fileSize;

    @ColumnInfo(name = "mime_type")
    public String mimeType;          // "image/jpeg" 등

    @ColumnInfo(name = "width")
    public int width;                // ✅ Photo 테이블에 width/height 있음

    @ColumnInfo(name = "height")
    public int height;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "is_deleted")
    public boolean isDeleted = false;

    @ColumnInfo(name = "server_photo_id")
    public Long serverPhotoId;
}
