# TikTok Live Blocker 🛡️

Aplikasi Android untuk memblokir fitur Live TikTok secara otomatis.

## Fitur
- ✅ Blokir UI Live TikTok via Accessibility Service
- 🚀 Auto-start saat HP dinyalakan (Boot Receiver)
- 🔔 Notifikasi saat ada yang coba buka Live
- 📋 Log aktivitas pemblokiran (max 100 entri)
- ⏰ Jadwal blokir aktif/nonaktif (misal 22:00–06:00)
- 🇮🇩 Antarmuka Bahasa Indonesia
- 📱 Support Android 8.0+ (API 26+)

## Cara Build APK

### Via GitHub Actions (Rekomendasi)
1. Upload folder ini ke repository GitHub
2. Buka tab **Actions**
3. Jalankan workflow **Build TikTok Live Blocker APK**
4. Download APK dari tab **Artifacts**

### Via Android Studio (Lokal)
1. Buka project di Android Studio
2. Tunggu Gradle sync selesai
3. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**
4. APK ada di `app/build/outputs/apk/debug/`

## Cara Install & Setup di HP

### 1. Install APK
- Aktifkan "Install dari sumber tidak dikenal" di Pengaturan HP
- Install APK

### 2. Aktifkan Accessibility Service (WAJIB)
```
Pengaturan HP → Aksesibilitas → Aplikasi yang Diunduh → TikTok Live Blocker → Aktifkan
```

### 3. Izinkan Notifikasi
Izinkan notifikasi saat diminta pertama kali.

### 4. Izinkan Autostart (khusus Xiaomi/OPPO/Vivo/Samsung)
```
Pengaturan → Aplikasi → TikTok Live Blocker → Autostart → Aktifkan
```

## Cara Kerja
1. **Accessibility Service** memantau setiap perpindahan layar di TikTok
2. Ketika layar Live terdeteksi (berdasarkan class Activity atau keyword UI), app langsung tekan Back
3. Notifikasi dikirim dan log dicatat
4. **WatchdogService** berjalan sebagai Foreground Service agar tidak dimatikan sistem
5. **BootReceiver** memulai ulang service saat HP dinyalakan

## Troubleshooting

| Masalah | Solusi |
|---|---|
| Service mati sendiri | Aktifkan Autostart di pengaturan HP |
| Tidak terdeteksi di Xiaomi | Aktifkan MIUI Optimization OFF |
| Live masih bisa dibuka | TikTok update activity class → hubungi developer |
| Notifikasi tidak muncul | Izinkan notifikasi di Pengaturan → Aplikasi |

## Struktur File
```
app/src/main/java/com/blocker/tiktok/
├── service/
│   ├── TikTokBlockerService.java   # Accessibility Service utama
│   └── WatchdogService.java        # Foreground service penjaga
├── receiver/
│   └── BootReceiver.java           # Auto-start saat boot
├── ui/
│   └── MainActivity.java           # UI utama
└── utils/
    ├── AppPreferences.java         # Penyimpanan pengaturan & log
    └── NotificationHelper.java     # Manajemen notifikasi
```
