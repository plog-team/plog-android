package com.example.plog.notification;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GeofenceManager {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    public static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 101;
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 102;

    private final Activity activity;

    public GeofenceManager(Activity activity) {
        this.activity = activity;
    }

    public void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            requestBackgroundLocationPermission();
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                );
            } else {
                requestNotificationPermission();
            }
        } else {
            requestNotificationPermission();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            } else {
                startLocationForegroundService();
            }
        } else {
            startLocationForegroundService();
        }
    }

    private void startLocationForegroundService() {
        Intent serviceIntent = new Intent(activity, LocationForegroundService.class);
        ContextCompat.startForegroundService(activity, serviceIntent);

        Log.d("LOCATION_SERVICE", "Foreground service start requested");
    }

    public void handlePermissionResult(int requestCode, int[] grantResults) {
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Log.e("LOCATION_PERMISSION", "Permission denied: " + requestCode);
            return;
        }

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            requestBackgroundLocationPermission();
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            requestNotificationPermission();
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            startLocationForegroundService();
        }
    }
}