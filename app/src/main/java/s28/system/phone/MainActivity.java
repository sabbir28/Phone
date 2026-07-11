package s28.system.phone;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import s28.system.phone.databinding.ActivityMainBinding;
import s28.system.phone.ui.CallLogFragment;
import s28.system.phone.ui.ContactsFragment;
import s28.system.phone.ui.DialerFragment;
import s28.system.phone.utils.DefaultAppManager;
import s28.system.phone.utils.PermissionManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, 0);
            
            // Adjust bottom navigation padding for navigation bar
            binding.bottomNavigation.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        setupBottomNavigation();
        
        if (!PermissionManager.hasAllPermissions(this)) {
            PermissionManager.requestPermissions(this);
        } else if (!DefaultAppManager.isDefaultDialer(this)) {
            DefaultAppManager.requestDefaultDialerRole(this);
        }

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(new DialerFragment());
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
