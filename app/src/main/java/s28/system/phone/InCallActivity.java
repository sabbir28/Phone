package s28.system.phone;

import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.content.Intent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.Locale;

import s28.system.phone.databinding.ActivityInCallBinding;

@RequiresApi(api = Build.VERSION_CODES.M)
public class InCallActivity extends AppCompatActivity {

    private ActivityInCallBinding binding;
    private CallManager callManager;
    private RecordingManager recordingManager;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long callStartTime = 0;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isIncomingRinging = false;
    private boolean pendingRecordStart = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (callStartTime > 0) {
                long duration = System.currentTimeMillis() - callStartTime;
                int seconds = (int) (duration / 1000);
                int minutes = seconds / 60;
                seconds %= 60;
                binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private final CallManager.CallListener callListener = new CallManager.CallListener() {
        @Override
        public void onCallAdded(Call call) {
            updateUI(call);
        }

        @Override
        public void onCallRemoved(Call call) {
            if (callManager.getCalls().isEmpty()) {
                stopTimer();
                finish();
            } else {
                updateUI(callManager.getCurrentCall());
            }
        }

        @Override
        public void onStateChanged(Call call, int state) {
            updateUI(call);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        binding = ActivityInCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        callManager = CallManager.getInstance();
        recordingManager = new RecordingManager();
        callManager.addListener(callListener);

        binding.btnAnswer.setOnClickListener(v -> callManager.answer());
        binding.btnHangup.setOnClickListener(v -> callManager.disconnect());
        
        binding.btnMute.setOnClickListener(v -> toggleMute());
        binding.btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        binding.btnRecord.setOnClickListener(v -> toggleRecording());
        
        binding.btnKeypad.setOnClickListener(v -> {
            // TODO: Implement keypad overlay
        });

        // Initialize speaker state from system
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            isSpeakerOn = audioManager.isSpeakerphoneOn();
            updateSpeakerButton();
        }

        updateUI(callManager.getCurrentCall());
    }

    private void updateUI(Call call) {
        List<Call> allCalls = callManager.getCalls();
        if (allCalls.isEmpty()) {
            stopTimer();
            finish();
            return;
        }

        if (call == null) call = allCalls.get(0);

        int state = call.getState();
        binding.callStatus.setText(stateToString(state));
        isIncomingRinging = state == Call.STATE_RINGING;

        android.net.Uri handle = call.getDetails().getHandle();
        String number = (handle != null) ? handle.getSchemeSpecificPart() : "Private Number";
        binding.callerName.setText(number);

        if (state == Call.STATE_ACTIVE) {
            startTimer();
            binding.tvTimer.setVisibility(View.VISIBLE);
            binding.callStatus.setVisibility(View.GONE);
            binding.btnAnswer.setVisibility(View.GONE);
            binding.btnHangup.setVisibility(View.VISIBLE);
        } else if (state == Call.STATE_RINGING) {
            stopTimer();
            binding.btnAnswer.setVisibility(View.VISIBLE);
            binding.btnHangup.setVisibility(View.VISIBLE); // End/Decline
            binding.tvTimer.setVisibility(View.GONE);
            binding.callStatus.setVisibility(View.VISIBLE);
        } else {
            stopTimer();
            binding.btnAnswer.setVisibility(View.GONE);
            binding.btnHangup.setVisibility(View.VISIBLE);
            binding.callStatus.setVisibility(View.VISIBLE);
        }

        updateRecordUI(state);
    }

    private void startTimer() {
        if (callStartTime == 0) {
            callStartTime = System.currentTimeMillis();
            timerHandler.post(timerRunnable);
        }
    }

    private void stopTimer() {
        callStartTime = 0;
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void toggleMute() {
        isMuted = !isMuted;
        callManager.setMute(isMuted);
        binding.btnMute.setSelected(isMuted);
        binding.btnMute.setBackgroundResource(isMuted ? R.drawable.ic_launcher_background : R.drawable.ic_fiber_manual_record); // Temporary highlight
        // iOS buttons usually change color when selected. For now I'll use a simple background change if possible.
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        
        // Use AudioManager for direct speaker control
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(isSpeakerOn);
        }
        
        // Also set via CallManager if available
        callManager.setAudioRoute(isSpeakerOn ? CallAudioState.ROUTE_SPEAKER : CallAudioState.ROUTE_EARPIECE);
        
        updateSpeakerButton();
    }

    private void updateSpeakerButton() {
        binding.btnSpeaker.setSelected(isSpeakerOn);
        if (isSpeakerOn) {
            binding.btnSpeaker.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#34C759")));
        } else {
            binding.btnSpeaker.setBackgroundTintList(null);
        }
    }

    private void toggleRecording() {
        if (recordingManager.isRecording()) {
            recordingManager.stopRecording();
            binding.tvRecordStatus.setText("Tap REC to start recording");
            binding.btnRecord.setText("REC");
            binding.btnRecord.setIcon(null);
            binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ios_blue, getTheme())));
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingRecordStart = true;
                Toast.makeText(this, "Microphone permission is required to record calls.", Toast.LENGTH_SHORT).show();
                s28.system.phone.utils.PermissionManager.requestPermissions(this);
                return;
            }
            startRecordingSession();
        }
    }

    private void startRecordingSession() {
        recordingManager.startRecording(this);
        if (recordingManager.isRecording()) {
            binding.tvRecordStatus.setText("Recording...");
            binding.btnRecord.setText("STOP");
            binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ios_cyan, getTheme())));
        } else {
            binding.tvRecordStatus.setText("Unable to start recording.");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        updateUI(callManager.getCurrentCall());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == s28.system.phone.utils.PermissionManager.PERMISSION_REQUEST_CODE) {
            boolean recordGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            if (recordGranted && pendingRecordStart) {
                startRecordingSession();
            } else if (!recordGranted) {
                Toast.makeText(this, "Recording permission denied.", Toast.LENGTH_SHORT).show();
            }
            pendingRecordStart = false;
        }
    }

    private void updateRecordUI(int state) {
        boolean activeCall = state == Call.STATE_ACTIVE;
        binding.btnRecord.setVisibility(activeCall ? View.VISIBLE : View.GONE);
        binding.tvRecordStatus.setVisibility(activeCall ? View.VISIBLE : View.GONE);

        if (!activeCall) {
            binding.tvRecordStatus.setText("Recording available once call is active.");
        } else if (recordingManager.isRecording()) {
            binding.tvRecordStatus.setText("Recording...");
            binding.btnRecord.setText("STOP");
            binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ios_tertiary, getTheme())));
        } else {
            binding.tvRecordStatus.setText("Tap REC to start recording");
            binding.btnRecord.setText("REC");
            binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ios_blue, getTheme())));
        }
    }

    private String stateToString(int state) {
        switch (state) {
            case Call.STATE_ACTIVE: return "active";
            case Call.STATE_RINGING: return "incoming call...";
            case Call.STATE_HOLDING: return "on hold";
            case Call.STATE_DIALING: return "calling...";
            case Call.STATE_CONNECTING: return "connecting...";
            case Call.STATE_DISCONNECTED: return "disconnected";
            case Call.STATE_DISCONNECTING: return "disconnecting...";
            case Call.STATE_NEW: return "new";
            default: return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (recordingManager != null && recordingManager.isRecording()) {
            recordingManager.stopRecording();
        }
        callManager.removeListener(callListener);
    }
}