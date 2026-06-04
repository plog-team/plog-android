// data/db/entity/UserEntity.java
package com.example.plog.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "user",
        indices = { @Index(value = "email", unique = true) }
)
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "nickname")
    public String nickname;

    @ColumnInfo(name = "profile_image_url")
    public String profileImageUrl;

    @ColumnInfo(name = "exchange_enabled")
    public boolean exchangeEnabled = false;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}