package s28.system.phone;

import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.InCallService;
import androidx.annotation.RequiresApi;
import s28.system.phone.beta.BetaSettings;

@RequiresApi(api = Build.VERSION_CODES.M)
public class DialerInCallService extends InCallService {

    private BetaSettings betaSettings;

    @Override
    public void onCreate() {
        super.onCreate();
        CallManager.getInstance().setInCallService(this);
        betaSettings = new BetaSettings(this);
    }

    private void handleCallState(Call call, int state) {
        if (state == Call.STATE_RINGING) {
            if (betaSettings.isFlashAlertEnabled()) {
                startFlashAlert();
            }
        } else {
            stopFlashAlert();
        }
    }

    private boolean isFlashing = false;
    private android.os.Handler flashHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable flashRunnable = new Runnable() {
        private boolean flashOn = false;
        @Override
        public void run() {
            if (!isFlashing) return;
            try {
                android.hardware.camera2.CameraManager cm = (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                String cameraId = cm.getCameraIdList()[0];
                flashOn = !flashOn;
                cm.setTorchMode(cameraId, flashOn);
                flashHandler.postDelayed(this, 500);
            } catch (Exception e) {
                android.util.Log.e("DialerInCallService", "Flash error", e);
                stopFlashAlert();
            }
        }
    };

    private void startFlashAlert() {
        if (isFlashing) return;
        isFlashing = true;
        flashHandler.post(flashRunnable);
    }

    private void stopFlashAlert() {
        if (!isFlashing) return;
        isFlashing = false;
        flashHandler.removeCallbacks(flashRunnable);
        try {
            android.hardware.camera2.CameraManager cm = (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
            String cameraId = cm.getCameraIdList()[0];
            cm.setTorchMode(cameraId, false);
        } catch (Exception ignored) {}
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        CallManager.getInstance().updateCall(call);
        
        handleCallState(call, call.getState());
        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                handleCallState(call, state);
            }
        });
        
        Intent intent = new Intent(this, InCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        if (betaSettings.isAutoRecordEnabled()) {
            intent.putExtra("EXTRA_AUTO_RECORD", true);
        }

        startActivity(intent);
    }

    @Override
    public void onCallAudioStateChanged(android.telecom.CallAudioState audioState) {
        super.onCallAudioStateChanged(audioState);
        // We could pass this to CallManager if we want to show mute/speaker state in UI
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        CallManager.getInstance().removeCall(call);
    }
}
