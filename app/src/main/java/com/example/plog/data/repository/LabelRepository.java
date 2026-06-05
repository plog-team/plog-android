// data/repository/LabelRepository.java
package com.example.plog.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.example.plog.data.db.AppDatabase;
import com.example.plog.data.db.dao.LabelDao;
import com.example.plog.data.db.dao.PhotoLabelDao;
import com.example.plog.data.db.entity.LabelEntity;
import com.example.plog.data.db.entity.PhotoLabelEntity;
import com.example.plog.model.LabelResult;
import com.example.plog.util.LabelTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LabelRepository {

    private final LabelDao        labelDao;
    private final PhotoLabelDao   photoLabelDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LabelRepository(@NonNull Context context) {
        AppDatabase db      = AppDatabase.getInstance(context);
        this.labelDao       = db.labelDao();
        this.photoLabelDao  = db.photoLabelDao();
    }

    /**
     * ML Kit 라벨 결과 → Label 테이블 upsert + PhotoLabel 저장
     *
     * 흐름:
     *   1. Label 테이블에 name INSERT IGNORE (중복 방지)
     *   2. 반환 id가 -1이면 이미 존재 → getIdByName()으로 id 조회
     *   3. PhotoLabel(photo_id, label_id, confidence) 배치 INSERT
     *
     * @param photoId 저장 완료된 photo.id
     * @param labels  ML Kit 분석 결과 (ImageLabelerHelper.onSuccess)
     */
    public void saveLabels(int photoId, @NonNull List<LabelResult> labels) {
        executor.execute(() -> insertLabelsSync(photoId, labels));
    }

    @WorkerThread
    private void insertLabelsSync(int photoId, @NonNull List<LabelResult> labels) {
        List<PhotoLabelEntity> photoLabels = new ArrayList<>();

        for (LabelResult result : labels) {

            // ① Label upsert
            LabelEntity labelEntity = new LabelEntity();
            labelEntity.name = LabelTranslator.translate(result.getText());

            long insertedId = labelDao.insertIgnore(labelEntity);

            int labelId;
            if (insertedId == -1) {
                // 이미 존재하는 라벨 → name으로 id 조회
                labelId = labelDao.getIdByName(labelEntity.name);
            } else {
                labelId = (int) insertedId;
            }

            if (labelId <= 0) continue;   // 예외 방어: id 조회 실패 시 스킵

            // ② PhotoLabel 생성
            PhotoLabelEntity photoLabel = new PhotoLabelEntity();
            photoLabel.photoId    = photoId;
            photoLabel.labelId    = labelId;
            photoLabel.confidence = result.getConfidence();
            photoLabels.add(photoLabel);
        }

        if (!photoLabels.isEmpty()) {
            photoLabelDao.insertAll(photoLabels);
        }
    }

    /**
     * 특정 사진의 라벨 목록 조회 (RecyclerView용 LiveData)
     */
    public LiveData<List<PhotoLabelDao.PhotoLabelResult>> getLabelsByPhotoId(int photoId) {
        return photoLabelDao.getLabelsByPhotoId(photoId);
    }
}