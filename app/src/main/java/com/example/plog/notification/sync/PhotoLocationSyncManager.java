package com.example.plog.notification.sync;

import android.content.Context;
import android.util.Log;

import com.example.plog.data.db.AppDatabase;
import com.example.plog.data.db.dao.PhotoDao;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.data.db.entity.PhotoEntity;
import com.example.plog.data.db.entity.PhotoLocationEntity;
import com.example.plog.model.ApiResponse;
import com.example.plog.network.RetrofitClient;
import com.example.plog.notification.api.PhotoLocationApi;
import com.example.plog.notification.dto.PhotoLocationResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoLocationSyncManager {

    public static void sync(Context context, long userId) {

        PhotoLocationApi api =
                RetrofitClient.getClient().create(PhotoLocationApi.class);

        api.getPhotoLocations(userId)
                .enqueue(new Callback<ApiResponse<List<PhotoLocationResponse>>>() {

                    @Override
                    public void onResponse(
                            Call<ApiResponse<List<PhotoLocationResponse>>> call,
                            Response<ApiResponse<List<PhotoLocationResponse>>> response
                    ) {

                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().data == null) {
                            return;
                        }
                        // Log.d("SYNC", "응답 개수 = " + response.body().data.size());

                        new Thread(() -> {

                            AppDatabase db = AppDatabase.getInstance(context);

                            PhotoDao photoDao = db.photoDao();
                            PhotoLocationDao locationDao = db.photoLocationDao();

                            for (PhotoLocationResponse item : response.body().data) {

                                /*
                                Log.d("SYNC",
                                        "server item: photoId="
                                                + item.photoId
                                                + ", lat=" + item.latitude
                                                + ", lng=" + item.longitude
                                                + ", location=" + item.locationName);
                                 */

                                Integer localPhotoId =
                                        photoDao.getLocalIdByServerPhotoIdSync(item.photoId);
                                /*
                                Log.d("SYNC",
                                        "localPhotoId = " + localPhotoId
                                                + ", serverPhotoId = " + item.photoId);

                                 */

                                if (localPhotoId == null) {

                                    PhotoEntity photo = new PhotoEntity();

                                    photo.userId = (int) userId;
                                    photo.imageUrl = "";
                                    photo.createdAt = System.currentTimeMillis();
                                    photo.serverPhotoId = (long) item.photoId;

                                    long insertedId = photoDao.insert(photo);
                                    // Log.d("SYNC", "photo insert 성공 insertedId = " + insertedId);

                                    localPhotoId = (int) insertedId;
                                }

                                locationDao.deleteByPhotoId(localPhotoId);

                                PhotoLocationEntity entity =
                                        new PhotoLocationEntity();

                                entity.photoId = localPhotoId;

                                entity.latitude = item.latitude;
                                entity.longitude = item.longitude;

                                entity.locationName = item.locationName;

                                if (item.takenAt != null && !item.takenAt.isEmpty()) {
                                    entity.takenAt =
                                            LocalDateTime.parse(item.takenAt)
                                                    .atZone(ZoneId.systemDefault())
                                                    .toInstant()
                                                    .toEpochMilli();
                                } else {
                                    entity.takenAt = System.currentTimeMillis();
                                    // Log.w("SYNC", "takenAt is null. photoId=" + item.photoId);
                                }
                                /*
                                Log.d("SYNC",
                                        "location insert 시도 photoId="
                                                + entity.photoId
                                                + ", lat=" + entity.latitude
                                                + ", lng=" + entity.longitude);

                                 */
                                locationDao.insert(entity);
                                /*
                                Log.d("SYNC",
                                        "photo location synced: "
                                                + entity.locationName);

                                Log.d("SYNC",
                                        "photo location synced: "
                                                + entity.locationName);

                                 */
                            }

                        }).start();
                    }

                    @Override
                    public void onFailure(
                            Call<ApiResponse<List<PhotoLocationResponse>>> call,
                            Throwable t
                    ) {
                        // Log.e("SYNC", "sync failed", t);
                    }
                });
    }
}
