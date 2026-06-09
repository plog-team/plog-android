package com.example.plog.notification;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DiaryReminderScheduler {

    public static void scheduleDailyDiaryReminder(Context context) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();

        target.set(Calendar.HOUR_OF_DAY, 22);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = target.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(
                        DailyDiaryReminderWorker.class,
                        1,
                        TimeUnit.DAYS
                )
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_diary_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );
    }

    public static void testDiaryReminderWorkerNow(Context context) {
        OneTimeWorkRequest testRequest =
                new OneTimeWorkRequest.Builder(DailyDiaryReminderWorker.class)
                        .build();

        WorkManager.getInstance(context).enqueue(testRequest);
    }
}
