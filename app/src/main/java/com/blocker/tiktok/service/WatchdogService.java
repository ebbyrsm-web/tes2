package com.blocker.tiktok.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;

import com.blocker.tiktok.utils.NotificationHelper;

public class WatchdogService extends Service {

    private Handler handler;
    private Runnable watchdogRunnable;
    private static final long CHECK_INTERVAL_MS = 30_000; // 30 detik

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        handler = new Handler(Looper.getMainLooper());

        watchdogRunnable = new Runnable() {
            @Override
            public void run() {
                checkAccessibilityServiceRunning();
                handler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start sebagai foreground service
        startForeground(
                NotificationHelper.NOTIF_FOREGROUND_ID,
                NotificationHelper.buildForegroundNotification(this, true)
        );

        // Mulai watchdog
        handler.post(watchdogRunnable);

        return START_STICKY; // Restart otomatis jika dimatikan sistem
    }

    private void checkAccessibilityServiceRunning() {
        // Cek apakah accessibility service aktif
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        boolean isEnabled = enabledServices != null &&
                enabledServices.contains("com.blocker.tiktok/.service.TikTokBlockerService");

        // Update notifikasi
        startForeground(
                NotificationHelper.NOTIF_FOREGROUND_ID,
                NotificationHelper.buildForegroundNotification(this, isEnabled)
        );
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // App di-swipe dari recent apps -> pastikan service tetap hidup.
        Intent restartIntent = new Intent(getApplicationContext(), WatchdogService.class);
        restartIntent.setPackage(getPackageName());
        getApplicationContext().startForegroundService(restartIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(watchdogRunnable);

        // Restart diri sendiri jika dimatikan
        Intent restartIntent = new Intent(getApplicationContext(), WatchdogService.class);
        restartIntent.setPackage(getPackageName());
        getApplicationContext().startForegroundService(restartIntent);
    }
}
