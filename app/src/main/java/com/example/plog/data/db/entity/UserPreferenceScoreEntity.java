package com.example.plog.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "user_preference_score",
        indices = { @Index(value = {"user_id", "category"}, unique = true) }
)
public class UserPreferenceScoreEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public int userId;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "score")
    public float score;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
