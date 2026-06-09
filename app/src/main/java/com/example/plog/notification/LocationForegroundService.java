package com.example.plog.notification;

import com.example.plog.data.db.AppDatabase;
import com.example.plog.data.db.entity.PhotoLocationEntity;
import com.example.plog.util.SessionManager;
import com.example.plog.util.Constants;

import com.example.plog.R;

import com.example.plog.data.db.dao.PhotoLocationDao;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationForegroundService extends Service {

    private static final String CHANNEL_ID = "location_service_channel";
    private static final int SERVICE_NOTIFICATION_ID = 2001;
    private static final float REVISIT_RADIUS = 1000f;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // 장소별 알림 여부 저장
    private final Set<Integer> notifiedLocationIds = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("P.")
                .setContentText("추억의 장소를 찾고 있어요")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        startForeground(SERVICE_NOTIFICATION_ID, notification);

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Log.e("LOCATION_SERVICE", "Location permission not granted");
            stopSelf();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000
        ).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location currentLocation : locationResult.getLocations()) {
                    /*
                    Log.d("LOCATION_SERVICE",
                            "Current: lat=" + currentLocation.getLatitude()
                                    + ", lng=" + currentLocation.getLongitude());
                     */

                    checkRevisitLocation(currentLocation);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                getMainLooper()
        );

        /* 마지막 위치로 즉시 한 번 검사
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d("LOCATION_SERVICE",
                                "LastLocation: lat=" + location.getLatitude()
                                        + ", lng=" + location.getLongitude());

                        checkRevisitLocation(location);
                    } else {
                        Log.d("LOCATION_SERVICE", "LastLocation is null");
                    }
                });

         */

        Log.d("LOCATION_SERVICE", "Started foreground location updates");
    }

    private void checkRevisitLocation(Location currentLocation) {
        new Thread(() -> {
            int rawUserId = new SessionManager(this).getUserId();
            int userId = rawUserId == -1 ? (int) Constants.DEV_USER_ID : rawUserId;

            // Log.d("LOCATION_SERVICE",
            //        "rawUserId = " + rawUserId + ", 조회 userId = " + userId);

            List<PhotoLocationDao.PhotoLocationWithImage> locations =
                    AppDatabase.getInstance(this)
                            .photoLocationDao()
                            .getAllWithLocationAndImageSync(userId);

            // Log.d("LOCATION_SERVICE", "저장된 사진 위치 개수: " + locations.size());

            PhotoLocationDao.PhotoLocationWithImage latestLocation = null;
            float latestDistance = -1f;

            for (PhotoLocationDao.PhotoLocationWithImage savedLocation : locations) {
                float[] result = new float[1];

                Location.distanceBetween(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude(),
                        savedLocation.latitude,
                        savedLocation.longitude,
                        result
                );

                float distance = result[0];

                /*
                Log.d("LOCATION_SERVICE",
                        "저장된 사진 위치 id=" + savedLocation.id
                                + ", lat=" + savedLocation.latitude
                                + ", lng=" + savedLocation.longitude
                                + ", distance=" + distance);
                */

                if (distance <= REVISIT_RADIUS) {
                    if (latestLocation == null
                            || savedLocation.takenAt > latestLocation.takenAt) {
                        latestLocation = savedLocation;
                        latestDistance = distance;
                    }
                } else {
                    notifiedLocationIds.remove(savedLocation.id);
                }
            }

            if (latestLocation != null) {
                if (!notifiedLocationIds.contains(latestLocation.id)) {

                    NotificationHelper.showRevisitNotification(this, latestLocation, latestDistance);
                    notifiedLocationIds.add(latestLocation.id);

                    //saveNotifiedToday(latestLocation.id);

                    // Log.d("LOCATION_SERVICE",
                    //        "최신 재방문 알림 표시 완료 id=" + latestLocation.id);
                }
            }

            /* 해당 장소에 대한 알림은 한 번만 오게
            if (latestLocation != null) {
                if (!notifiedLocationIds.contains(latestLocation.id)
                        && !isAlreadyNotifiedToday(latestLocation.id)) {

                    NotificationHelper.showRevisitNotification(this, latestLocation, latestDistance);
                    notifiedLocationIds.add(latestLocation.id);
                    saveNotifiedToday(latestLocation.id);

                    Log.d("LOCATION_SERVICE",
                            "최신 재방문 알림 표시 완료 id=" + latestLocation.id);
                }
            }

             */
        }).start();
    }




    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Foreground Service",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    (NotificationManager) getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        notifiedLocationIds.clear();

        // Log.d("LOCATION_SERVICE", "Foreground service stopped");
    }

    private boolean isAlreadyNotifiedToday(int locationId) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
                .format(new java.util.Date());

        String key = "revisit_notified_" + locationId;

        return getSharedPreferences("revisit_notification", MODE_PRIVATE)
                .getString(key, "")
                .equals(today);
    }

    private void saveNotifiedToday(int locationId) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
                .format(new java.util.Date());

        String key = "revisit_notified_" + locationId;

        getSharedPreferences("revisit_notification", MODE_PRIVATE)
                .edit()
                .putString(key, today)
                .apply();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}