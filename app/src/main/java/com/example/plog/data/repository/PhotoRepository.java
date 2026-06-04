// data/repository/PhotoRepository.java
package com.example.plog.data.repository;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.example.plog.data.db.AppDatabase;
import com.example.plog.data.db.dao.PhotoDao;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.data.db.entity.PhotoEntity;
import com.example.plog.data.db.entity.PhotoLocationEntity;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.PhotoUploadBatchResponse;
import com.example.plog.network.ApiClient;
import com.example.plog.util.ExifExtractor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class PhotoRepository {

    private final PhotoDao         photoDao;
    private final PhotoLocationDao locationDao;
    private final ExifExtractor    exifExtractor;
    private final ExecutorService  executor       = Executors.newSingleThreadExecutor();
    // 서버 업로드 전용 스레드풀 — DB 작업 executor를 블로킹하지 않도록 분리
    private final ExecutorService  uploadExecutor = Executors.newCachedThreadPool();

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

    public LiveData<List<PhotoLocationDao.PhotoLocationWithImage>> getMonthlyLocationsWithImage(
            int userId, long startMs, long endMs) {
        return locationDao.getMonthlyLocationsWithImage(userId, startMs, endMs);
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

            // 역지오코딩 — locationName / address 업데이트
            reverseGeocode(context, (int) photoId, loc.latitude, loc.longitude);
        } else {
            Log.d("PhotoRepository", "GPS 없음 — PhotoLocation 저장 안 함");
        }

        // ── 서버 업로드 — 별도 스레드로 분리해 DB executor 블로킹 방지 ───────
        final Uri uploadUri = uri;
        uploadExecutor.execute(() -> uploadToServer(uploadUri, context));

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

    /** 일기 수정 시 교체된 사진 로컬 소프트 삭제 + 서버 삭제 */
    @WorkerThread
    public void softDeleteByImageUrl(String imageUrl) {
        Long serverPhotoId = photoDao.getServerPhotoIdByImageUrl(imageUrl);
        photoDao.softDeleteByImageUrl(imageUrl);
        if (serverPhotoId != null) {
            deleteFromServer(serverPhotoId);
        }
    }

    /** GPS 있으나 location_name 없는 항목에 역지오코딩 일괄 적용 */
    @WorkerThread
    public void backfillLocationNames(@NonNull Context context) {
        List<PhotoLocationEntity> entries = locationDao.getMissingLocationNames();
        for (PhotoLocationEntity entry : entries) {
            reverseGeocode(context, entry.photoId, entry.latitude, entry.longitude);
        }
        Log.d("PhotoRepository", "backfill 완료 — " + entries.size() + "건 처리");
    }

    /** 특정 일기의 사진 목록 (팀원 DiaryPhoto 연동용) */
    public LiveData<List<PhotoEntity>> getPhotosByUser(int userId) {
        return photoDao.getByUser(userId);
    }

    /** 갤러리 URI → 서버 업로드. 실패해도 로컬 저장에 영향 없음. */
    @WorkerThread
    private void uploadToServer(@NonNull Uri uri, @NonNull Context context) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);

            String mime = context.getContentResolver().getType(uri);
            if (mime == null) mime = "image/jpeg";
            String filename = "photo_" + System.currentTimeMillis() + ".jpg";

            RequestBody body = RequestBody.create(baos.toByteArray(), MediaType.parse(mime));
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, body);

            Response<ApiResponse<PhotoUploadBatchResponse>> response =
                    ApiClient.getApiService().uploadPhoto(part).execute();

            if (response.isSuccessful() && response.body() != null
                    && response.body().data != null
                    && response.body().data.photos != null
                    && !response.body().data.photos.isEmpty()) {
                long serverPhotoId = response.body().data.photos.get(0).photoId;
                photoDao.updateServerPhotoId(uri.toString(), serverPhotoId);
                Log.d("PhotoRepository", "서버 업로드 완료 — serverPhotoId: " + serverPhotoId);
            } else {
                Log.w("PhotoRepository", "서버 업로드 실패: HTTP " + response.code());
            }
        } catch (Exception e) {
            Log.w("PhotoRepository", "서버 업로드 실패 (무시): " + e.getMessage());
        }
    }

    @WorkerThread
    private void deleteFromServer(long serverPhotoId) {
        try {
            Response<Void> response = ApiClient.getApiService().deletePhoto(serverPhotoId).execute();
            if (response.isSuccessful()) {
                Log.d("PhotoRepository", "서버 사진 삭제 완료 — serverPhotoId: " + serverPhotoId);
            } else {
                Log.w("PhotoRepository", "서버 사진 삭제 실패: HTTP " + response.code());
            }
        } catch (Exception e) {
            Log.w("PhotoRepository", "서버 사진 삭제 실패 (무시): " + e.getMessage());
        }
    }

    /**
     * 위도/경도 → 장소명 + 주소 역지오코딩 후 DB 업데이트.
     * 이미 @WorkerThread 에서 호출되므로 동기 Geocoder 사용.
     */
    @WorkerThread
    @SuppressWarnings("deprecation")
    private void reverseGeocode(@NonNull Context context, int photoId,
                                double latitude, double longitude) {
        if (!Geocoder.isPresent()) {
            Log.d("PhotoRepository", "Geocoder 사용 불가 — 역지오코딩 생략");
            return;
        }
        try {
            Geocoder geocoder = new Geocoder(context, Locale.KOREA);
            List<Address> results = geocoder.getFromLocation(latitude, longitude, 1);
            if (results == null || results.isEmpty()) {
                Log.d("PhotoRepository", "역지오코딩 결과 없음");
                return;
            }

            Address addr = results.get(0);

            // 장소명: 숫자 번지가 아닌 featureName → 동/구 → 시 순으로 폴백
            String feature = addr.getFeatureName();
            String locationName;
            if (feature != null && !feature.matches("\\d+.*")) {
                locationName = feature;
            } else {
                String sub      = addr.getSubLocality();   // 동
                String locality = addr.getLocality();       // 시
                String admin    = addr.getAdminArea();      // 도
                locationName = sub != null ? sub
                        : (locality != null ? locality : admin);
            }

            String address = addr.getMaxAddressLineIndex() >= 0
                    ? addr.getAddressLine(0) : "";

            if (locationName != null) {
                locationDao.updateLocationName(photoId, locationName, address);
                Log.d("PhotoRepository", "역지오코딩 완료 — "
                        + locationName + " / " + address);
            }
        } catch (IOException e) {
            Log.w("PhotoRepository", "역지오코딩 실패: " + e.getMessage());
        }
    }
    /** 갤러리 URI로 서버 photoId 조회 */
    @WorkerThread
    public Long getServerPhotoIdByImageUrl(String imageUrl) {
        return photoDao.getServerPhotoIdByImageUrl(imageUrl);
    }
}