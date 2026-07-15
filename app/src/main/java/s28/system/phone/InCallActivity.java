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
import android.media.projection.MediaProjectionManager;
import android.content.Intent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private boolean isSystemRecordingMode = false;
    private boolean isGoogleRecordingMode = false;

    private ActivityResultLauncher<Intent> projectionLauncher;

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

        projectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        startSystemRecording(result.getResultCode(), result.getData());
                    } else {
                        Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        binding.btnAnswer.setOnClickListener(v -> callManager.answer());
        binding.btnHangup.setOnClickListener(v -> callManager.disconnect());
        
        binding.btnMute.setOnClickListener(v -> toggleMute());
        binding.btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        binding.btnRecord.setOnClickListener(v -> toggleRecording());
        
        binding.btnKeypad.setOnClickListener(v -> showKeypad());
        
        setupKeypad();

        // Initialize speaker state from system
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            isSpeakerOn = audioManager.isSpeakerphoneOn();
            updateSpeakerButton();
        }

        updateUI(callManager.getCurrentCall());

        // Check for Auto Record (Beta Feature)
        if (getIntent().getBooleanExtra("EXTRA_AUTO_RECORD", false)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!recordingManager.isRecording()) {
                    startRecordingSession();
                }
            }, 1000); // Small delay to ensure call is active
        }
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
            binding.btnHangup.setText("End Call");
            binding.buttonGrid.setVisibility(View.VISIBLE);
        } else if (state == Call.STATE_RINGING) {
            stopTimer();
            binding.btnAnswer.setVisibility(View.VISIBLE);
            binding.btnHangup.setVisibility(View.VISIBLE);
            binding.btnHangup.setText("Decline");
            binding.tvTimer.setVisibility(View.GONE);
            binding.callStatus.setVisibility(View.VISIBLE);
            binding.buttonGrid.setVisibility(View.INVISIBLE);
        } else {
            stopTimer();
            binding.btnAnswer.setVisibility(View.GONE);
            binding.btnHangup.setVisibility(View.VISIBLE);
            binding.btnHangup.setText("End Call");
            binding.callStatus.setVisibility(View.VISIBLE);
            binding.buttonGrid.setVisibility(View.INVISIBLE);
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
            String duration = binding.tvTimer.getText().toString();
            Call currentCall = callManager.getCurrentCall();
            String direction = "Unknown";
            if (currentCall != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    int dir = currentCall.getDetails().getCallDirection();
                    direction = (dir == android.telecom.Call.Details.DIRECTION_INCOMING) ? "Incoming 📥" : "Outgoing 📤";
                } else {
                    // Fallback for older versions
                    direction = isIncomingRinging ? "Incoming 📥" : "Outgoing 📤";
                }
            }
            
            recordingManager.stopRecording(this, duration, direction);
            
            if (isGoogleRecordingMode) {
                if (currentCall != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    currentCall.sendCallEvent("android.telecom.event.STOP_CALL_RECORDING", null);
                }
            }

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
            
            // For Android 10+, show choice or default to system recording if user wants "everything"
            // For now, let's use a long click to trigger system recording or just a simple toggle.
            // I will implement a simple dialog or just default to standard and use system as secondary.
            showRecordingOptions();
        }
    }

    private void showRecordingOptions() {
        String[] options = {"Record with Original API (Recommended)", "Record System Audio (Screen Capture)"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Recording Mode")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        isSystemRecordingMode = false;
                        isGoogleRecordingMode = false;
                        startRecordingSession();
                    } else {
                        isSystemRecordingMode = true;
                        isGoogleRecordingMode = false;
                        requestSystemRecording();
                    }
                })
                .show();
    }

    private void requestSystemRecording() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (projectionManager != null) {
            projectionLauncher.launch(projectionManager.createScreenCaptureIntent());
        }
    }

    private void startSystemRecording(int resultCode, Intent data) {
        Call currentCall = callManager.getCurrentCall();
        String number = "Unknown";
        if (currentCall != null && currentCall.getDetails().getHandle() != null) {
            number = currentCall.getDetails().getHandle().getSchemeSpecificPart();
        }
        recordingManager.startSystemRecording(this, number, resultCode, data);
        updateRecordUI(currentCall != null ? currentCall.getState() : Call.STATE_ACTIVE);
    }

    private void startRecordingSession() {
        Call currentCall = callManager.getCurrentCall();
        String number = "Unknown";
        if (currentCall != null && currentCall.getDetails().getHandle() != null) {
            number = currentCall.getDetails().getHandle().getSchemeSpecificPart();
        }
        
        recordingManager.startRecording(this, number, isGoogleRecordingMode);
        
        if (isGoogleRecordingMode && currentCall != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                // Send Google Dialer compatible recording event
                currentCall.sendCallEvent("android.telecom.event.START_CALL_RECORDING", null);
                // Also try common OEM variations
                currentCall.sendCallEvent("com.google.android.dialer.callrecording.START_RECORDING", null);
            } catch (Exception e) {
                android.util.Log.e("InCallActivity", "Failed to send call recording event", e);
            }
        }

        if (recordingManager.isRecording()) {
            binding.tvRecordStatus.setText("Recording Active...");
            binding.btnRecord.setText("STOP");
            binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ios_cyan, getTheme())));
        } else {
            binding.tvRecordStatus.setText("Recording Failed to Start.");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        updateUI(callManager.getCurrentCall());

        // Check for Auto Record (Beta Feature)
        if (getIntent().getBooleanExtra("EXTRA_AUTO_RECORD", false)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!recordingManager.isRecording()) {
                    startRecordingSession();
                }
            }, 1000); // Small delay to ensure call is active
        }
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
        Call currentCall = callManager.getCurrentCall();
        if (currentCall != null) {
            // CAPABILITY_CAN_RECORD_CALL is 0x00800000 in API 23+
            boolean canRecord = (currentCall.getDetails().getCallCapabilities() & 0x00800000) != 0;
            android.util.Log.d("InCallActivity", "System capability can record: " + canRecord);
        }

        boolean activeCall = state == Call.STATE_ACTIVE;
        binding.btnRecord.setVisibility(activeCall ? View.VISIBLE : View.GONE);
        binding.tvRecordStatus.setVisibility(activeCall ? View.VISIBLE : View.GONE);

        if (!activeCall) {
            binding.tvRecordStatus.setText("Recording available once call is active.");
        } else if (recordingManager.isRecording()) {
            binding.tvRecordStatus.setText("Recording...");
            binding.btnRecord.setText("STOP");
            binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.light_on_surface_variant, getTheme())));
        } else {
            binding.tvRecordStatus.setText("Tap REC to start recording");
            binding.btnRecord.setText("REC");
            binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.ios_blue, getTheme())));
        }
    }

    private void showKeypad() {
        View overlay = findViewById(R.id.keypadOverlay);
        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideKeypad() {
        View overlay = findViewById(R.id.keypadOverlay);
        if (overlay != null) {
            overlay.setVisibility(View.GONE);
            TextView tvDigits = findViewById(R.id.tvDigitsDisplay);
            if (tvDigits != null) tvDigits.setText("");
        }
    }

    private void setupKeypad() {
        View overlay = findViewById(R.id.keypadOverlay);
        if (overlay == null) return;

        findViewById(R.id.btnHideKeypad).setOnClickListener(v -> hideKeypad());

        setupDtmfButton(R.id.btnDtmf1, '1');
        setupDtmfButton(R.id.btnDtmf2, '2');
        setupDtmfButton(R.id.btnDtmf3, '3');
        setupDtmfButton(R.id.btnDtmf4, '4');
        setupDtmfButton(R.id.btnDtmf5, '5');
        setupDtmfButton(R.id.btnDtmf6, '6');
        setupDtmfButton(R.id.btnDtmf7, '7');
        setupDtmfButton(R.id.btnDtmf8, '8');
        setupDtmfButton(R.id.btnDtmf9, '9');
        setupDtmfButton(R.id.btnDtmf0, '0');
        setupDtmfButton(R.id.btnDtmfStar, '*');
        setupDtmfButton(R.id.btnDtmfPound, '#');
    }

    private void setupDtmfButton(int id, char digit) {
        View buttonView = findViewById(id);
        if (buttonView == null) return;

        TextView tvDigit = buttonView.findViewById(R.id.tvDtmfDigit);
        if (tvDigit != null) tvDigit.setText(String.valueOf(digit));

        buttonView.setOnTouchListener((v, event) -> {
            Call call = callManager.getCurrentCall();
            if (call == null) return false;

            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    call.playDtmfTone(digit);
                    updateDtmfDisplay(digit);
                    v.setPressed(true);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    call.stopDtmfTone();
                    v.setPressed(false);
                    return true;
            }
            return false;
        });
    }

    private void updateDtmfDisplay(char digit) {
        TextView tvDigits = findViewById(R.id.tvDigitsDisplay);
        if (tvDigits != null) {
            String current = tvDigits.getText().toString();
            tvDigits.setText(current + digit);
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
            String duration = binding.tvTimer.getText().toString();
            Call currentCall = callManager.getCurrentCall();
            String direction = "Unknown";
            if (currentCall != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    int dir = currentCall.getDetails().getCallDirection();
                    direction = (dir == android.telecom.Call.Details.DIRECTION_INCOMING) ? "Incoming 📥" : "Outgoing 📤";
                } else {
                    // Fallback for older versions
                    direction = isIncomingRinging ? "Incoming 📥" : "Outgoing 📤";
                }
            }
            recordingManager.stopRecording(this, duration, direction);
        }
        callManager.removeListener(callListener);
    }
}