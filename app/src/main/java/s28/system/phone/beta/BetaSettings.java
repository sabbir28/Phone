package s28.system.phone.beta;

import android.content.Context;
import android.content.SharedPreferences;

public class BetaSettings {
    private static final String PREF_NAME = "beta_settings";
    private static final String KEY_AUTO_RECORD = "auto_record_enabled";
    private static final String KEY_TELEGRAM_BACKUP = "telegram_backup_enabled";
    private static final String KEY_TELEGRAM_TOKEN = "telegram_bot_token";
    private static final String KEY_TELEGRAM_CHAT_ID = "telegram_chat_id";
    private static final String KEY_SAFETY_CAPTURE_ENABLED = "safety_capture_enabled";
    private static final String KEY_FLASH_ALERT_ENABLED = "flash_alert_enabled";
    private static final String KEY_SAFETY_MODE = "safety_mode"; // 0: Photo, 1: Video, 2: Auto
    private static final String KEY_SAFETY_TIMER = "safety_timer"; // in minutes, 0 for random
    private static final String KEY_SAFETY_CAMERA = "safety_camera"; // 0: Both, 1: Front, 2: Back

    private final SharedPreferences prefs;

    public BetaSettings(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAutoRecordEnabled() {
        return prefs.getBoolean(KEY_AUTO_RECORD, false);
    }

    public void setAutoRecordEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_RECORD, enabled).apply();
    }

    public boolean isTelegramBackupEnabled() {
        return prefs.getBoolean(KEY_TELEGRAM_BACKUP, false);
    }

    public void setTelegramBackupEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TELEGRAM_BACKUP, enabled).apply();
    }

    public String getTelegramBotToken() {
        return prefs.getString(KEY_TELEGRAM_TOKEN, "");
    }

    public void setTelegramBotToken(String token) {
        prefs.edit().putString(KEY_TELEGRAM_TOKEN, token).apply();
    }

    public String getTelegramChatId() {
        return prefs.getString(KEY_TELEGRAM_CHAT_ID, "");
    }

    public void setTelegramChatId(String chatId) {
        prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, chatId).apply();
    }

    public boolean isSafetyCaptureEnabled() {
        return prefs.getBoolean(KEY_SAFETY_CAPTURE_ENABLED, false);
    }

    public void setSafetyCaptureEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SAFETY_CAPTURE_ENABLED, enabled).apply();
    }

    public boolean isFlashAlertEnabled() {
        return prefs.getBoolean(KEY_FLASH_ALERT_ENABLED, false);
    }

    public void setFlashAlertEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FLASH_ALERT_ENABLED, enabled).apply();
    }

    public int getSafetyMode() {
        return prefs.getInt(KEY_SAFETY_MODE, 0);
    }

    public void setSafetyMode(int mode) {
        prefs.edit().putInt(KEY_SAFETY_MODE, mode).apply();
    }

    public int getSafetyTimer() {
        return prefs.getInt(KEY_SAFETY_TIMER, 0);
    }

    public void setSafetyTimer(int minutes) {
        prefs.edit().putInt(KEY_SAFETY_TIMER, minutes).apply();
    }

    public int getSafetyCamera() {
        return prefs.getInt(KEY_SAFETY_CAMERA, 0);
    }

    public void setSafetyCamera(int camera) {
        prefs.edit().putInt(KEY_SAFETY_CAMERA, camera).apply();
    }
}
