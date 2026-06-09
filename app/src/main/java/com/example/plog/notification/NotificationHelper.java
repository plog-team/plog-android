package com.example.plog.notification;

import com.example.plog.MainActivity;
import com.example.plog.R;
import com.example.plog.data.db.entity.PhotoLocationEntity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;

import android.util.Log;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.plog.data.DiaryEntry;
import com.example.plog.data.DiaryRepository;
import com.example.plog.data.db.dao.PhotoLocationDao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.graphics.BitmapFactory;
import com.example.plog.util.Constants;
import java.net.HttpURLConnection;
import java.net.URL;

public class NotificationHelper {

    private static final String REVISIT_CHANNEL_ID = "diary_revisit_channel";
    private static final String REVISIT_CHANNEL_NAME = "Diary Revisit Notifications";

    private static final String REMINDER_CHANNEL_ID = "diary_write_reminder_channel";
    private static final String REMINDER_CHANNEL_NAME = "Diary Write Reminder Notifications";

    // 기존 코드 호환용
    public static void showRevisitNotification(Context context) {
        showRevisitNotification(context, null, -1f);
    }

    // 실제 재방문 알림
    @SuppressLint("MissingPermission")
    public static void showRevisitNotification(
            Context context,
            PhotoLocationDao.PhotoLocationWithImage location,
            float distance
    ) {
        createChannel(
                context,
                REVISIT_CHANNEL_ID,
                REVISIT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        /*
        if (location != null && location.diaryId > 0) {
            openIntent.putExtra("openDiaryId", location.diaryId);
        }
        */

        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent dismissIntent = new Intent(context, NotificationDismissReceiver.class);

        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String placeName = "추억의 장소";
        String address = "이곳에서의 추억을 만나보세요.";

        if (location != null) {
            if (location.locationName != null && !location.locationName.isEmpty()) {
                placeName = location.locationName;
            }

            if (location.address != null && !location.address.isEmpty()) {
                address = location.address;
            }
        }

        String distanceText = "";
        if (distance >= 0) {
            distanceText = " · 약 " + Math.round(distance) + "m 근처";
        }

        long takenAt = location != null ? location.takenAt : System.currentTimeMillis();

        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                .format(new Date(takenAt));

        openIntent.putExtra("openDiaryDate", dateKey);
        openIntent.putExtra("cancelNotificationId", 1001);

        DiaryEntry diary = new DiaryRepository(context).getDiary(dateKey);

        String title = diary != null && diary.getTitle() != null && !diary.getTitle().isEmpty()
                ? diary.getTitle()
                : "추억의 장소에 도착했어요";

        String place = placeName != null && !placeName.isEmpty()
                ? placeName
                : address;

        String displayDate = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                .format(new Date(takenAt));

        Bitmap diaryImage = null;

        String imageUrl = null;

        if (location != null && location.serverPhotoId != null) {
            String baseUrl = Constants.BASE_URL.endsWith("/")
                    ? Constants.BASE_URL
                    : Constants.BASE_URL + "/";

            imageUrl = baseUrl + "api/photos/" + location.serverPhotoId;
        } else if (location != null) {
            imageUrl = location.imageUrl;
        }

        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty(
                            Constants.HEADER_USER_ID,
                            String.valueOf(Constants.DEV_USER_ID)
                    );
                    connection.connect();

                    diaryImage = BitmapFactory.decodeStream(connection.getInputStream());
                    connection.disconnect();
                } else {
                    diaryImage = MediaStore.Images.Media.getBitmap(
                            context.getContentResolver(),
                            Uri.parse(imageUrl)
                    );
                }
            }
        } catch (Exception e) {
            Log.w("NotificationHelper", "알림 이미지 로드 실패: " + e.getMessage());
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, REVISIT_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("\uD83D\uDCF8 방문한 적 있는 장소예요")
                        .setContentText("     이곳에서의 추억을 만나보세요")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(openPendingIntent);

        if (diaryImage != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(diaryImage)
                    .setBigContentTitle(title)
                    .setSummaryText("🗓️ " + displayDate + " · 📍 " + place));
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("🗓️ " + displayDate + " · 📍 " + place));
        }

        builder.addAction(R.mipmap.ic_launcher, "닫기", dismissPendingIntent)
                        .addAction(R.mipmap.ic_launcher, "확인하기", openPendingIntent);

        if (!hasNotificationPermission(context)) {
            return;
        }

        NotificationManagerCompat.from(context).notify(1001, builder.build());
    }

    // 일기 작성 알림
    @SuppressLint("MissingPermission")
    public static void showDiaryWriteReminderNotification(Context context) {
        createChannel(
                context,
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        openIntent.putExtra("openWriteDiary", true);
        openIntent.putExtra("cancelNotificationId", 2002);

        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                2002,
                openIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent dismissIntent = new Intent(context, NotificationDismissReceiver.class);

        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("✨ 어떤 하루를 보내셨나요?")
                        .setContentText("    오늘의 순간을 기록해보세요!")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(openPendingIntent)
                        .addAction(R.mipmap.ic_launcher, "닫기", dismissPendingIntent)
                        .addAction(R.mipmap.ic_launcher, "작성하기", openPendingIntent);

        if (!hasNotificationPermission(context)) {
            return;
        }

        NotificationManagerCompat.from(context).notify(2002, builder.build());
    }

    private static void createChannel(
            Context context,
            String channelId,
            String channelName,
            int importance
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(channelId, channelName, importance);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            manager.createNotificationChannel(channel);
        }
    }

    private static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }
}