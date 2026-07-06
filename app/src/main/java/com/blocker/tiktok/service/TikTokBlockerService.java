package com.blocker.tiktok.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.blocker.tiktok.utils.AppPreferences;
import com.blocker.tiktok.utils.NotificationHelper;

import java.util.Arrays;
import java.util.List;

public class TikTokBlockerService extends AccessibilityService {

    // Package name TikTok di berbagai region
    private static final List<String> TIKTOK_PACKAGES = Arrays.asList(
            "com.zhiliaoapp.musically",   // TikTok global
            "com.ss.android.ugc.trill",   // TikTok di beberapa region Asia
            "com.tiktok.app"              // Variant lain
    );

    // Keyword yang menandakan layar Live TikTok
    private static final List<String> LIVE_KEYWORDS = Arrays.asList(
            "live",
            "LIVE",
            "Live",
            "mulai live",
            "go live",
            "siaran langsung",
            "live streaming",
            "start live"
    );

    // Class name Activity Live TikTok (diketahui dari reverse engineering)
    private static final List<String> LIVE_ACTIVITY_CLASSES = Arrays.asList(
            "com.ss.android.ugc.aweme.live.LivePlayActivity",
            "com.ss.android.ugc.aweme.live.ui.LiveEntranceActivity",
            "com.ss.android.ugc.aweme.live.ui.LiveBroadcastActivity",
            "com.zhiliaoapp.musically.live.LivePlayActivity",
            "com.zhiliaoapp.musically.live.ui.LiveBroadcastActivity"
    );

    private AppPreferences prefs;
    private Handler handler;
    private long lastBlockTime = 0;
    private static final long BLOCK_COOLDOWN_MS = 400; // Jeda antar aksi blokir, cukup singkat agar tetap responsif

    // Polling berkelanjutan selagi TikTok di foreground, sebagai lapisan kedua
    // di luar event accessibility (yang kadang tidak terpicu tepat waktu).
    private static final long POLL_INTERVAL_MS = 500;
    private boolean isTikTokForeground = false;
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTikTokForeground) {
                checkWindowForLiveContent();
                handler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new AppPreferences(this);
        handler = new Handler(Looper.getMainLooper());
        NotificationHelper.createChannels(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // Catatan: proteksi ini SELALU aktif, tanpa jadwal dan tanpa saklar
        // on/off. Semua akun live diblokir, kapan pun, tanpa kecuali.

        // Cek apakah event dari TikTok
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;

        String pkg = packageName.toString();
        boolean isTikTok = false;
        for (String tikTokPkg : TIKTOK_PACKAGES) {
            if (pkg.equals(tikTokPkg)) {
                isTikTok = true;
                break;
            }
        }

        if (!isTikTok) {
            // Keluar dari TikTok -> hentikan polling
            if (isTikTokForeground) {
                isTikTokForeground = false;
                handler.removeCallbacks(pollRunnable);
            }
            return;
        }

        // Masuk/berada di TikTok -> pastikan polling berjalan
        if (!isTikTokForeground) {
            isTikTokForeground = true;
            handler.removeCallbacks(pollRunnable);
            handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        }

        int eventType = event.getEventType();

        // Periksa saat window/activity berpindah
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String className = event.getClassName() != null ? event.getClassName().toString() : "";
            checkAndBlockLiveActivity(className, event);
        }

        // Periksa saat konten berubah / scroll / klik (deteksi tombol & badge Live)
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            checkWindowForLiveContent();
        }
    }

    private void checkAndBlockLiveActivity(String className, AccessibilityEvent event) {
        // Cek berdasarkan class name Activity
        for (String liveClass : LIVE_ACTIVITY_CLASSES) {
            if (className.equals(liveClass) || className.contains("Live") || className.contains("live")) {
                if (className.contains("Broadcast") || className.contains("broadcast") ||
                        className.contains("Play") || className.contains("Entrance") ||
                        className.contains("Live")) {
                    blockLive("Layar Live TikTok terdeteksi");
                    return;
                }
            }
        }

        // Cek berdasarkan teks event
        CharSequence text = event.getText() != null && !event.getText().isEmpty()
                ? event.getText().get(0) : null;
        if (text != null) {
            String textStr = text.toString().toLowerCase();
            for (String keyword : LIVE_KEYWORDS) {
                if (textStr.contains(keyword.toLowerCase())) {
                    checkWindowForLiveContent();
                    break;
                }
            }
        }
    }

    private void checkWindowForLiveContent() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        if (containsLiveNode(root)) {
            blockLive("Tombol/konten Live TikTok ditemukan");
        }
        root.recycle();
    }

    private boolean containsLiveNode(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // Periksa text node
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        String textStr = text != null ? text.toString() : "";
        String descStr = desc != null ? desc.toString() : "";

        // Cek apakah ini adalah elemen live streaming (siaran)
        for (String keyword : LIVE_KEYWORDS) {
            if (textStr.equalsIgnoreCase(keyword) || descStr.equalsIgnoreCase(keyword)) {
                // Periksa apakah ini konteks broadcast (bukan sekedar teks "live")
                String viewId = node.getViewIdResourceName() != null
                        ? node.getViewIdResourceName() : "";
                if (viewId.contains("live") || viewId.contains("broadcast") ||
                        textStr.equalsIgnoreCase("live") || textStr.equalsIgnoreCase("mulai live") ||
                        textStr.equalsIgnoreCase("go live")) {
                    return true;
                }
            }
        }

        // Rekursif ke child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (containsLiveNode(child)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    private static final int MAX_BACK_RETRIES = 3;

    private void blockLive(String reason) {
        long now = System.currentTimeMillis();
        if (now - lastBlockTime < BLOCK_COOLDOWN_MS) return;
        lastBlockTime = now;

        // Catat log dan kirim notifikasi SEKALI per kejadian
        prefs.incrementBlockCount();
        prefs.addLog("Diblokir: " + reason);
        NotificationHelper.sendBlockAlert(this,
                "Percobaan membuka Live TikTok telah diblokir. Total: " + prefs.getBlockCount() + "x");

        Intent broadcast = new Intent("com.blocker.tiktok.BLOCK_EVENT");
        broadcast.putExtra("count", prefs.getBlockCount());
        sendBroadcast(broadcast);

        // Coba keluar dari layar live, dan pastikan benar-benar keluar
        attemptExitLive(0);
    }

    /**
     * Tekan BACK berulang untuk keluar dari layar live. Jika setelah beberapa
     * kali percobaan konten live masih terdeteksi (mis. dialog konfirmasi
     * "keluar dari live" atau layar live yang tidak menutup dengan BACK),
     * fallback ke GLOBAL_ACTION_HOME supaya live tidak mungkin tetap tampil.
     */
    private void attemptExitLive(int retryCount) {
        performGlobalAction(GLOBAL_ACTION_BACK);

        handler.postDelayed(() -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            boolean stillLive = root != null && containsLiveNode(root);
            if (root != null) root.recycle();

            if (stillLive) {
                if (retryCount < MAX_BACK_RETRIES) {
                    attemptExitLive(retryCount + 1);
                } else {
                    // BACK tidak cukup (misal live full-screen tanpa navigasi back
                    // yang jelas) -> paksa keluar ke Home agar live tidak tampil.
                    performGlobalAction(GLOBAL_ACTION_HOME);
                }
            }
        }, 250);
    }

    @Override
    public void onInterrupt() {
        // Service terganggu
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
    }
}
