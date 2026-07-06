package com.blocker.tiktok.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.blocker.tiktok.R;
import com.blocker.tiktok.ui.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_FOREGROUND = "ch_foreground";
    public static final String CHANNEL_BLOCK_ALERT = "ch_block_alert";
    public static final int NOTIF_FOREGROUND_ID = 1001;
    public static final int NOTIF_BLOCK_ALERT_ID = 1002;

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);

            // Channel foreground service (silent)
            NotificationChannel foregroundChannel = new NotificationChannel(
                    CHANNEL_FOREGROUND,
                    "Status Layanan",
                    NotificationManager.IMPORTANCE_LOW
            );
            foregroundChannel.setDescription("Menampilkan status TikTok Live Blocker aktif");
            foregroundChannel.setShowBadge(false);

            // Channel alert pemblokiran
            NotificationChannel blockChannel = new NotificationChannel(
                    CHANNEL_BLOCK_ALERT,
                    "Peringatan Pemblokiran",
                    NotificationManager.IMPORTANCE_HIGH
            );
            blockChannel.setDescription("Notifikasi saat TikTok Live diblokir");
            blockChannel.enableVibration(true);

            nm.createNotificationChannel(foregroundChannel);
            nm.createNotificationChannel(blockChannel);
        }
    }

    public static Notification buildForegroundNotification(Context context, boolean isActive) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String status = isActive ? "Aktif — memantau TikTok" : "Terjadwal — tidak aktif saat ini";

        return new NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
                .setContentTitle("TikTok Live Blocker")
                .setContentText(status)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public static void sendBlockAlert(Context context, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("tab", "log");
        PendingIntent pi = PendingIntent.getActivity(context, 1, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notif = new NotificationCompat.Builder(context, CHANNEL_BLOCK_ALERT)
                .setContentTitle("⛔ TikTok Live Diblokir")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .build();

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_BLOCK_ALERT_ID, notif);
        } catch (SecurityException e) {
            // Permission notification belum diberikan
        }
    }
}
