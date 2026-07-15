package s28.system.phone;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AppCompatActivity;
import s28.system.phone.beta.BetaSettings;
import s28.system.phone.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private BetaSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settings = new BetaSettings(this);

        setupUI();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAbout.setOnClickListener(v -> {
            startActivity(new Intent(this, AboutActivity.class));
        });

        // Telegram Test Button
        binding.btnTestTelegram.setOnClickListener(v -> {
            String token = binding.etBotToken.getText().toString().trim();
            String chatId = binding.etChatId.getText().toString().trim();

            if (token.isEmpty() || chatId.isEmpty()) {
                android.widget.Toast.makeText(this, "Please enter both Token and Chat ID first", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnTestTelegram.setEnabled(false);
            binding.btnTestTelegram.setText("Testing Connection...");

            new s28.system.phone.beta.TelegramBackupManager(this).sendTestMessage(
                "✅ Connection test from Phone App beta!",
                (success, message) -> runOnUiThread(() -> {
                    binding.btnTestTelegram.setEnabled(true);
                    binding.btnTestTelegram.setText("Test Telegram Connection");
                    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
                })
            );
        });

        // Auto Record
        binding.switchAutoRecord.setChecked(settings.isAutoRecordEnabled());
        binding.switchAutoRecord.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setAutoRecordEnabled(isChecked);
        });

        // Safety Capture
        binding.switchSafetyCapture.setChecked(settings.isSafetyCaptureEnabled());
        binding.switchSafetyCapture.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setSafetyCaptureEnabled(isChecked);
            if (isChecked) {
                Intent intent = new Intent(this, SafetyCaptureService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            } else {
                stopService(new Intent(this, SafetyCaptureService.class));
            }
        });

        // Safety Mode (Photo/Video/Auto)
        int mode = settings.getSafetyMode();
        if (mode == 0) binding.rbPhoto.setChecked(true);
        else if (mode == 1) binding.rbVideo.setChecked(true);
        else binding.rbAuto.setChecked(true);
        
        binding.rgSafetyMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPhoto) settings.setSafetyMode(0);
            else if (checkedId == R.id.rbVideo) settings.setSafetyMode(1);
            else if (checkedId == R.id.rbAuto) settings.setSafetyMode(2);
        });

        // Safety Camera (Both/Front/Back)
        int cam = settings.getSafetyCamera();
        if (cam == 0) binding.rbBoth.setChecked(true);
        else if (cam == 1) binding.rbFront.setChecked(true);
        else binding.rbBack.setChecked(true);

        binding.rgSafetyCamera.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbBoth) settings.setSafetyCamera(0);
            else if (checkedId == R.id.rbFront) settings.setSafetyCamera(1);
            else if (checkedId == R.id.rbBack) settings.setSafetyCamera(2);
        });

        // Safety Timer
        binding.etSafetyTimer.setText(String.valueOf(settings.getSafetyTimer()));
        binding.etSafetyTimer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    int val = Integer.parseInt(s.toString());
                    settings.setSafetyTimer(val);
                } catch (Exception ignored) {}
            }
        });

        // Flash Alert
        binding.switchFlashAlert.setChecked(settings.isFlashAlertEnabled());
        binding.switchFlashAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setFlashAlertEnabled(isChecked);
        });

        // Telegram Backup
        binding.switchTelegramBackup.setChecked(settings.isTelegramBackupEnabled());
        binding.switchTelegramBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setTelegramBackupEnabled(isChecked);
        });

        binding.etBotToken.setText(settings.getTelegramBotToken());
        binding.etBotToken.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                settings.setTelegramBotToken(s.toString());
            }
        });

        binding.etChatId.setText(settings.getTelegramChatId());
        binding.etChatId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                settings.setTelegramChatId(s.toString());
            }
        });
    }
}
