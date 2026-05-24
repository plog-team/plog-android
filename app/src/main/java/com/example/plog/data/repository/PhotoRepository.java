// data/repository/PhotoRepository.java
package com.example.plog.data.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.example.plog.data.db.AppDatabase;
import com.example.plog.data.db.dao.PhotoDao;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.data.db.entity.PhotoEntity;
import com.example.plog.data.db.entity.PhotoLocationEntity;
import com.example.plog.util.ExifExtractor;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoRepository {

    private final PhotoDao         photoDao;
    private final PhotoLocationDao locationDao;
    private final ExifExtractor    exifExtractor;
    private final ExecutorService  executor = Executors.newSingleThreadExecutor();

    public PhotoRepository(@NonNull Context context) {
        AppDatabase db      = AppDatabase.getInstance(context);
        this.photoDao       = db.photoDao();
        this.locationDao    = db.photoLocationDao();
        this.exifExtractor  = new ExifExtractor();
    }

    // PhotoRepository.java 에 추가
    public LiveData<List<PhotoLocationDao.PhotoLocationWithImage>> getAllLocationsWithImage(int userId) {
        return locationDao.getAllWithLocationAndImage(userId);
    }

    // ── [문제 1 수정] saveLabels() 완전 제거 → LabelRepository로 이동 ──────

    /**
     * 갤러리 URI → Photo + PhotoLocation 저장
     * EXIF 추출 포함. IO 스레드에서 호출 필요 (@WorkerThread)
     *
     * @param uri     갤러리 URI (PickVisualMedia 결과)
     * @param userId  현재 로그인 유저 id (SessionManager.getUserId())
     * @param context context
     * @return 저장된 photo.id (-1이면 실패)
     */
    @WorkerThread
    public long savePhoto(@NonNull Uri uri, int userId, @NonNull Context context) {

        // ── Android 10 이상 — 원본 EXIF 접근을 위한 URI 변환 ──────────────
        Uri exifUri = uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exifUri = MediaStore.setRequireOriginal(uri);
        }

        ExifExtractor.ExifResult exif = null;

        try {
            // 원본 URI로 EXIF 추출 시도
            exif = exifExtractor.extract(exifUri, context);
        } catch (Exception e) {
            // 원본 URI 실패 시 일반 URI로 재시도
            Log.w("PhotoRepository", "원본 URI 실패 — 일반 URI로 재시도" + e.getMessage());
            exif = exifExtractor.extract(uri, context);
        }

        // ── Photo 저장 ──────────────────────────────────────────────────────
        PhotoEntity photo = new PhotoEntity();
        photo.userId    = userId;
        photo.imageUrl  = uri.toString();
        photo.createdAt = System.currentTimeMillis();
        photo.isDeleted = false;

        if (exif != null) {
            photo.width  = exif.imageWidth;
            photo.height = exif.imageHeight;
        }

        long photoId = photoDao.insert(photo);
        Log.d("PhotoRepository", "Photo 저장 완료 — id: " + photoId);

        // ── PhotoLocation 저장 ─────────────────────────────────────────────
        if (exif != null && exif.hasLocation()) {
            PhotoLocationEntity loc = new PhotoLocationEntity();
            loc.photoId   = (int) photoId;
            loc.takenAt   = exif.hasTakenAt() ? exif.takenAtMs : System.currentTimeMillis();
            loc.latitude  = exif.latitude;
            loc.longitude = exif.longitude;
            locationDao.insert(loc);
            Log.d("PhotoRepository", "GPS 저장 완료 — lat: "
                    + loc.latitude + " lng: " + loc.longitude);
        } else {
            Log.d("PhotoRepository", "GPS 없음 — PhotoLocation 저장 안 함");
        }

        return photoId;
    }

    /**
     * 역지오코딩 결과를 PhotoLocation에 업데이트
     * Geocoder 작업 완료 후 호출
     */
    @WorkerThread
    public void updateLocationName(int photoId,
                                   @NonNull String locationName,
                                   @NonNull String address) {
        locationDao.updateLocationName(photoId, locationName, address);
    }

    /** 지도 화면: 해당 유저의 GPS 있는 사진 전체 (LiveData) */
    public LiveData<List<PhotoLocationEntity>> getAllLocations(int userId) {
        return locationDao.getAllWithLocationLive(userId);
    }

    /** 특정 일기의 사진 목록 (팀원 DiaryPhoto 연동용) */
    public LiveData<List<PhotoEntity>> getPhotosByUser(int userId) {
        return photoDao.getByUser(userId);
    }
}