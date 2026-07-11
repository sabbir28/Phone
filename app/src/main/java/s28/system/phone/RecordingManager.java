package s28.system.phone;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingManager {
    private static final String TAG = "RecordingManager";
    private MediaRecorder recorder;
    private boolean isRecording = false;
    private String currentFilePath;

    public void startRecording(Context context) {
        if (isRecording) return;

        File recordDir = new File(context.getExternalFilesDir(null), "Recordings");
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        currentFilePath = recordDir.getAbsolutePath() + "/Call_" + timeStamp + ".amr";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setOutputFile(currentFilePath);

        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            Log.d(TAG, "Recording started: " + currentFilePath);
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "start() failed", e);
        }
    }

    public void stopRecording() {
        if (!isRecording || recorder == null) return;

        try {
            recorder.stop();
            recorder.release();
        } catch (RuntimeException e) {
            Log.e(TAG, "stop() failed", e);
        } finally {
            recorder = null;
            isRecording = false;
            Log.d(TAG, "Recording stopped");
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}
