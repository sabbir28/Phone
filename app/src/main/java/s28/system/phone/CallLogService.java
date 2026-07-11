package s28.system.phone;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

public class CallLogService extends Service {
    private static final String TAG = "CallLogService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (action != null) {
            String number = intent.getStringExtra("phone_number");
            switch (action) {
                case "LOG_CALL_START":
                    Log.i(TAG, "Call started with: " + number);
                    // Here you would typically save to a database or File
                    break;
                case "LOG_CALL_END":
                    Log.i(TAG, "Call ended with: " + number);
                    break;
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
