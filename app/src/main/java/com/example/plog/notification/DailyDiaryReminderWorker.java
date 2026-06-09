package com.example.plog.notification;

import android.content.Context;

import com.example.plog.data.DiaryRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DailyDiaryReminderWorker extends Worker {

    public DailyDiaryReminderWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {

        android.util.Log.d("DIARY_REMINDER", "Daily diary reminder worker executed");

        DiaryRepository repository =
                new DiaryRepository(getApplicationContext());

        String today =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());

        boolean hasTodayDiary =
                repository.getDiary(today) != null;

        if (!hasTodayDiary) {
            NotificationHelper.showDiaryWriteReminderNotification(
                    getApplicationContext()
            );
        }

        return Result.success();
    }

    /* 테스트용 - doWork()를 아래 코드를 살리면 '일기 없음' 상태 -> 알림 띄움
    @NonNull
    @Override
    public Result doWork() {

        android.util.Log.d("DIARY_REMINDER", "Daily diary reminder worker executed");

        boolean hasTodayDiary = false;

        if (!hasTodayDiary) {
            NotificationHelper.showDiaryWriteReminderNotification(getApplicationContext());
        }

        return Result.success();
    }
     */


}