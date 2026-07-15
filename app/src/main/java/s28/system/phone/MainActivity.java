package s28.system.phone;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.core.splashscreen.SplashScreen;
import s28.system.phone.databinding.ActivityMainBinding;
import s28.system.phone.ui.CallLogFragment;
import s28.system.phone.ui.ContactsFragment;
import s28.system.phone.ui.DialerFragment;
import s28.system.phone.beta.BetaSettings;
import android.content.Intent;
import s28.system.phone.utils.DefaultAppManager;
import s28.system.phone.utils.PermissionManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Top padding for status bar
            v.setPadding(insets.left, insets.top, insets.right, 0);
            
            // Adjust bottom navigation margin to be above system navigation bar
            // We use margin to keep it "floating" above the nav bar
            android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) binding.bottomNavigation.getLayoutParams();
            lp.bottomMargin = insets.bottom + (int)(16 * getResources().getDisplayMetrics().density);
            binding.bottomNavigation.setLayoutParams(lp);

            return windowInsets;
        });

        setupBottomNavigation();
        
        if (!PermissionManager.hasAllPermissions(this)) {
            PermissionManager.requestPermissions(this);
        } else if (!DefaultAppManager.isDefaultDialer(this)) {
            DefaultAppManager.requestDefaultDialerRole(this);
        }

        // Check for battery optimization
        if (!s28.system.phone.utils.BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            s28.system.phone.utils.BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this);
        }

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(new DialerFragment());
        }

        // Start Safety Capture if enabled
        BetaSettings settings = new BetaSettings(this);
        if (settings.isSafetyCaptureEnabled()) {
            Intent intent = new Intent(this, SafetyCaptureService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dialer) {
                loadFragment(new DialerFragment());
                return true;
            } else if (itemId == R.id.nav_call_log) {
                loadFragment(new CallLogFragment());
                return true;
            } else if (itemId == R.id.nav_contacts) {
                loadFragment(new ContactsFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            if (PermissionManager.hasAllPermissions(this) && !DefaultAppManager.isDefaultDialer(this)) {
                DefaultAppManager.requestDefaultDialerRole(this);
            }
        }
    }
}
