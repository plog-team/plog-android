package com.example.plog.data.db;
// data/db/AppDatabase.java
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.plog.data.db.dao.LabelDao;
import com.example.plog.data.db.dao.PhotoDao;
import com.example.plog.data.db.dao.PhotoLabelDao;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.data.db.entity.LabelEntity;
import com.example.plog.data.db.entity.PhotoEntity;
import com.example.plog.data.db.entity.PhotoLabelEntity;
import com.example.plog.data.db.entity.PhotoLocationEntity;
import com.example.plog.data.db.entity.UserEntity;

@Database(
        entities = {
                UserEntity.class,          // ← 추가
                PhotoEntity.class,
                PhotoLocationEntity.class,
                LabelEntity.class,
                PhotoLabelEntity.class
                // 팀원 담당: UserEntity, DiaryEntity, DiaryPhotoEntity 등 추가 예정
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract PhotoDao         photoDao();
    public abstract PhotoLocationDao photoLocationDao();
    public abstract LabelDao         labelDao();
    public abstract PhotoLabelDao    photoLabelDao();


    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "diary_app.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}