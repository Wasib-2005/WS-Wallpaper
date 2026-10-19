package com.wasib.wallpaper;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

final class RotationScheduler {
    static final String WORK_NAME = "ws-wallpaper-rotation";
    static final long MIN_INTERVAL_MINUTES = 15;

    private RotationScheduler() {
    }

    static void start(Context context, long intervalSeconds) {
        long minutes = Math.max(MIN_INTERVAL_MINUTES,
                (long) Math.ceil(intervalSeconds / 60.0));
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                WallpaperWorker.class, minutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    static void stop(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
}
