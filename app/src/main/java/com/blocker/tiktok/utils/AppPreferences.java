package com.blocker.tiktok.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppPreferences {

    private static final String PREF_NAME = "tiktok_blocker_prefs";
    private static final String KEY_BLOCKER_ENABLED = "blocker_enabled";
    private static final String KEY_SCHEDULE_ENABLED = "schedule_enabled";
    private static final String KEY_SCHEDULE_START_HOUR = "schedule_start_hour";
    private static final String KEY_SCHEDULE_START_MIN = "schedule_start_min";
    private static final String KEY_SCHEDULE_END_HOUR = "schedule_end_hour";
    private static final String KEY_SCHEDULE_END_MIN = "schedule_end_min";
    private static final String KEY_BLOCK_COUNT = "block_count";
    private static final String KEY_LOGS = "block_logs";
    private static final int MAX_LOGS = 100;

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isBlockerEnabled() {
        return prefs.getBoolean(KEY_BLOCKER_ENABLED, true);
    }

    public void setBlockerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BLOCKER_ENABLED, enabled).apply();
    }

    public boolean isScheduleEnabled() {
        return prefs.getBoolean(KEY_SCHEDULE_ENABLED, false);
    }

    public void setScheduleEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply();
    }

    public int getScheduleStartHour() {
        return prefs.getInt(KEY_SCHEDULE_START_HOUR, 22);
    }

    public int getScheduleStartMin() {
        return prefs.getInt(KEY_SCHEDULE_START_MIN, 0);
    }

    public void setScheduleStart(int hour, int min) {
        prefs.edit()
                .putInt(KEY_SCHEDULE_START_HOUR, hour)
                .putInt(KEY_SCHEDULE_START_MIN, min)
                .apply();
    }

    public int getScheduleEndHour() {
        return prefs.getInt(KEY_SCHEDULE_END_HOUR, 6);
    }

    public int getScheduleEndMin() {
        return prefs.getInt(KEY_SCHEDULE_END_MIN, 0);
    }

    public void setScheduleEnd(int hour, int min) {
        prefs.edit()
                .putInt(KEY_SCHEDULE_END_HOUR, hour)
                .putInt(KEY_SCHEDULE_END_MIN, min)
                .apply();
    }

    public int getBlockCount() {
        return prefs.getInt(KEY_BLOCK_COUNT, 0);
    }

    public void incrementBlockCount() {
        int count = getBlockCount() + 1;
        prefs.edit().putInt(KEY_BLOCK_COUNT, count).apply();
    }

    public void resetBlockCount() {
        prefs.edit().putInt(KEY_BLOCK_COUNT, 0).apply();
    }

    public void addLog(String message) {
        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = timestamp + " — " + message;

        Set<String> existingLogs = prefs.getStringSet(KEY_LOGS, new HashSet<>());
        List<String> logList = new ArrayList<>(existingLogs);

        logList.add(0, logEntry);
        if (logList.size() > MAX_LOGS) {
            logList = logList.subList(0, MAX_LOGS);
        }

        prefs.edit().putStringSet(KEY_LOGS, new HashSet<>(logList)).apply();
    }

    public List<String> getLogs() {
        Set<String> logSet = prefs.getStringSet(KEY_LOGS, new HashSet<>());
        List<String> logList = new ArrayList<>(logSet);
        // Sort by newest first (timestamp string sortable)
        logList.sort((a, b) -> b.compareTo(a));
        return logList;
    }

    public void clearLogs() {
        prefs.edit().remove(KEY_LOGS).remove(KEY_BLOCK_COUNT).apply();
    }

    /**
     * Cek apakah waktu sekarang masuk dalam jadwal blokir aktif
     */
    public boolean isInActiveSchedule() {
        if (!isScheduleEnabled()) return true; // Jika jadwal dinonaktifkan, selalu aktif

        java.util.Calendar cal = java.util.Calendar.getInstance();
        int nowHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int nowMin = cal.get(java.util.Calendar.MINUTE);
        int nowTotal = nowHour * 60 + nowMin;

        int startTotal = getScheduleStartHour() * 60 + getScheduleStartMin();
        int endTotal = getScheduleEndHour() * 60 + getScheduleEndMin();

        if (startTotal > endTotal) {
            // Jadwal melewati tengah malam (misal 22:00 - 06:00)
            return nowTotal >= startTotal || nowTotal < endTotal;
        } else {
            return nowTotal >= startTotal && nowTotal < endTotal;
        }
    }
}
