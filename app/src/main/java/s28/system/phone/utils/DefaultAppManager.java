package s28.system.phone.utils;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.TelecomManager;

public class DefaultAppManager {
    public static final int REQUEST_CODE_SET_DEFAULT_DIALER = 123;

    public static boolean isDefaultDialer(Context context) {
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        return telecomManager.getDefaultDialerPackage().equals(context.getPackageName());
    }

    public static void requestDefaultDialerRole(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) activity.getSystemService(Context.ROLE_SERVICE);
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                activity.startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER);
            }
        } else {
            Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.getPackageName());
            activity.startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER);
        }
    }
}
