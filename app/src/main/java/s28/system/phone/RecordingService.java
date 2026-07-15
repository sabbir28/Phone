package s28.system.phone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RecordingService extends Service {
    private static final String TAG = "RecordingService";
    private static final String CHANNEL_ID = "RecordingServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_DATA = "EXTRA_DATA";
    public static final String EXTRA_FILE_PATH = "EXTRA_FILE_PATH";

    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private int trackIndex = -1;
    private boolean isRecording = false;
    private Thread recordingThread;
    
    private final Executor callbackExecutor = Executors.newSingleThreadExecutor();
    private AudioManager.AudioRecordingCallback audioRecordingCallback;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
                Intent data = intent.getParcelableExtra(EXTRA_DATA);
                String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
                startRecording(resultCode, data, filePath);
            } else if (ACTION_STOP.equals(action)) {
                stopRecording();
            }
        }
        return START_NOT_STICKY;
    }

    private void startRecording(int resultCode, Intent data, String filePath) {
        if (isRecording) return;

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Recording Audio")
                .setContentText("System audio capture is in progress")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = projectionManager.getMediaProjection(resultCode, data);

        if (mediaProjection == null) {
            Log.e(TAG, "Failed to get MediaProjection");
            stopSelf();
            return;
        }

        try {
            setupAudioCapture(filePath);
            isRecording = true;
            recordingThread = new Thread(this::recordLoop);
            recordingThread.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to setup audio capture", e);
            stopSelf();
        }
    }

    private void setupAudioCapture(String filePath) throws IOException {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build();

            AudioRecord.Builder builder = new AudioRecord.Builder()
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .setAudioPlaybackCaptureConfig(config);
            
            // setPrivacySensitive is API 30+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setPrivacySensitive(true);
            }

            try {
                audioRecord = builder.build();
            } catch (SecurityException | UnsupportedOperationException e) {
                Log.e(TAG, "Failed to build AudioRecord: " + e.getMessage());
                throw new IOException("AudioRecord creation failed", e);
            }

            registerRecordingCallback();
        }

        if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IOException("Failed to initialize AudioRecord");
        }

        mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
        
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    private void registerRecordingCallback() {
        if (audioRecord == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        
        audioRecordingCallback = new AudioManager.AudioRecordingCallback() {
            @Override
            public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                super.onRecordingConfigChanged(configs);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    for (AudioRecordingConfiguration config : configs) {
                        if (config.getClientAudioSessionId() == audioRecord.getAudioSessionId()) {
                            if (config.isClientSilenced()) {
                                Log.w(TAG, "Recording has been SILENCED by the system (priority conflict)");
                            } else {
                                Log.d(TAG, "Recording is active (not silenced)");
                            }
                            break;
                        }
                    }
                }
            }
        };
        audioRecord.registerAudioRecordingCallback(callbackExecutor, audioRecordingCallback);
    }

    private void recordLoop() {
        if (audioRecord == null) return;
        
        try {
            audioRecord.startRecording();
        } catch (IllegalStateException e) {
            Log.e(TAG, "startRecording failed", e);
            isRecording = false;
            return;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        long presentationTimeUs = 0;

        while (isRecording) {
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                int bytesRead = audioRecord.read(inputBuffer, inputBuffer.capacity());
                if (bytesRead > 0) {
                    presentationTimeUs = System.nanoTime() / 1000;
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, 0);
                } else {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, 0);
                }
            }

            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = mediaMuxer.addTrack(mediaCodec.getOutputFormat());
                mediaMuxer.start();
            } else if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0 && trackIndex != -1) {
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                }
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        }
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        try {
            if (recordingThread != null) recordingThread.join(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error joining thread", e);
        }

        if (audioRecord != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && audioRecordingCallback != null) {
                audioRecord.unregisterAudioRecordingCallback(audioRecordingCallback);
            }
            try {
                audioRecord.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audioRecord", e);
            }
            audioRecord.release();
            audioRecord = null;
        }
        if (mediaCodec != null) {
            try {
                mediaCodec.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping mediaCodec", e);
            }
            mediaCodec.release();
            mediaCodec = null;
        }
        if (mediaMuxer != null) {
            try {
                mediaMuxer.stop();
            } catch (Exception e) {
                Log.e(TAG, "Muxer stop failed", e);
            }
            mediaMuxer.release();
            mediaMuxer = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recording Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
