package com.blocker.tiktok.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.blocker.tiktok.R;
import com.blocker.tiktok.service.WatchdogService;
import com.blocker.tiktok.utils.AppPreferences;
import com.blocker.tiktok.utils.NotificationHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppPreferences prefs;
    private TextView tvStatus;
    private TextView tvBlockCount;
    private TextView tvLogs;
    private Switch switchBlocker;
    private Switch switchSchedule;
    private LinearLayout layoutSchedule;
    private TextView tvScheduleStart;
    private TextView tvScheduleEnd;
    private ImageView ivStatusIcon;
    private Button btnEnableAccessibility;
    private Button btnClearLogs;

    private BroadcastReceiver blockEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new AppPreferences(this);
        NotificationHelper.createChannels(this);

        initViews();
        setupListeners();

        // Mulai WatchdogService
        Intent serviceIntent = new Intent(this, WatchdogService.class);
        startForegroundService(serviceIntent);

        // Minta izin notifikasi Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvBlockCount = findViewById(R.id.tv_block_count);
        tvLogs = findViewById(R.id.tv_logs);
        switchBlocker = findViewById(R.id.switch_blocker);
        switchSchedule = findViewById(R.id.switch_schedule);
        layoutSchedule = findViewById(R.id.layout_schedule);
        tvScheduleStart = findViewById(R.id.tv_schedule_start);
        tvScheduleEnd = findViewById(R.id.tv_schedule_end);
        ivStatusIcon = findViewById(R.id.iv_status_icon);
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        btnClearLogs = findViewById(R.id.btn_clear_logs);
    }

    private void setupListeners() {
        // Proteksi selalu aktif: tidak ada jadwal dan tidak bisa dimatikan
        // lewat UI. Satu-satunya cara berhenti adalah mematikan perangkat.
        prefs.setBlockerEnabled(true);
        prefs.setScheduleEnabled(false);
        switchBlocker.setChecked(true);
        switchBlocker.setEnabled(false);
        switchSchedule.setChecked(false);
        switchSchedule.setEnabled(false);
        layoutSchedule.setVisibility(View.GONE);

        btnEnableAccessibility.setOnClickListener(v -> {
            showAccessibilityGuide();
        });

        btnClearLogs.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Hapus Log")
                    .setMessage("Yakin ingin menghapus semua log pemblokiran?")
                    .setPositiveButton("Hapus", (dialog, which) -> {
                        prefs.clearLogs();
                        updateUI();
                        Toast.makeText(this, "Log berhasil dihapus", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    private void showTimePicker(boolean isStart) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timepicker, null);
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);

        int hour = isStart ? prefs.getScheduleStartHour() : prefs.getScheduleEndHour();
        int min = isStart ? prefs.getScheduleStartMin() : prefs.getScheduleEndMin();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(min);
        }

        new AlertDialog.Builder(this)
                .setTitle(isStart ? "Waktu Mulai Blokir" : "Waktu Akhir Blokir")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int h, m;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        h = timePicker.getHour();
                        m = timePicker.getMinute();
                    } else {
                        h = timePicker.getCurrentHour();
                        m = timePicker.getCurrentMinute();
                    }
                    if (isStart) prefs.setScheduleStart(h, m);
                    else prefs.setScheduleEnd(h, m);
                    updateScheduleText();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showAccessibilityGuide() {
        new AlertDialog.Builder(this)
                .setTitle("Aktifkan Accessibility Service")
                .setMessage(
                        "Langkah-langkah:\n\n" +
                        "1. Tap tombol 'Buka Pengaturan' di bawah\n" +
                        "2. Cari 'TikTok Live Blocker'\n" +
                        "3. Aktifkan dengan menggeser toggle\n" +
                        "4. Tap 'Izinkan' pada konfirmasi\n\n" +
                        "Fitur ini diperlukan agar aplikasi dapat memantau dan memblokir Live TikTok."
                )
                .setPositiveButton("Buka Pengaturan", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Nanti", null)
                .show();
    }

    private void updateUI() {
        boolean accessibilityOn = isAccessibilityServiceEnabled();

        // Status icon dan teks. Tidak ada lagi status "dinonaktifkan" atau
        // "terjadwal" karena proteksi selalu aktif tanpa kecuali.
        if (!accessibilityOn) {
            ivStatusIcon.setImageResource(R.drawable.ic_warning);
            tvStatus.setText("⚠️ Accessibility Service belum aktif — aktifkan agar proteksi berjalan");
            tvStatus.setTextColor(getColor(R.color.status_warning));
            btnEnableAccessibility.setVisibility(View.VISIBLE);
        } else {
            ivStatusIcon.setImageResource(R.drawable.ic_shield);
            tvStatus.setText("✅ Selalu aktif — semua Live TikTok diblokir");
            tvStatus.setTextColor(getColor(R.color.status_active));
            btnEnableAccessibility.setVisibility(View.GONE);
        }

        // Counter
        tvBlockCount.setText("Total diblokir: " + prefs.getBlockCount() + "x");

        // Logs
        updateLogs();
    }

    private void updateLogs() {
        List<String> logs = prefs.getLogs();
        if (logs.isEmpty()) {
            tvLogs.setText("Belum ada aktivitas pemblokiran.");
        } else {
            StringBuilder sb = new StringBuilder();
            int max = Math.min(logs.size(), 20);
            for (int i = 0; i < max; i++) {
                sb.append("• ").append(logs.get(i)).append("\n");
            }
            tvLogs.setText(sb.toString().trim());
        }
    }

    private void updateScheduleText() {
        String start = String.format("%02d:%02d",
                prefs.getScheduleStartHour(), prefs.getScheduleStartMin());
        String end = String.format("%02d:%02d",
                prefs.getScheduleEndHour(), prefs.getScheduleEndMin());
        tvScheduleStart.setText("Mulai: " + start);
        tvScheduleEnd.setText("Selesai: " + end);
    }

    private boolean isAccessibilityServiceEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return enabledServices != null &&
                enabledServices.contains("com.blocker.tiktok");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        registerReceiver(blockEventReceiver,
                new IntentFilter("com.blocker.tiktok.BLOCK_EVENT"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(blockEventReceiver);
        } catch (Exception ignored) {}
    }
}
