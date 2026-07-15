package s28.system.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class BatteryLevelReceiver extends BroadcastReceiver {
    private static boolean isShowing = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) (level * 100 / (float) scale);

            if (batteryPct <= 15 && !isShowing) {
                showBatteryPopup(context, batteryPct);
            } else if (batteryPct > 15) {
                isShowing = false;
            }
        }
    }

    private void showBatteryPopup(Context context, int percent) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return;

        isShowing = true;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP;
        params.y = 100;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_battery_warning, null);

        TextView tvTitle = popupView.findViewById(R.id.tvPopupTitle);
        TextView tvMsg = popupView.findViewById(R.id.tvPopupMessage);
        Button btnDismiss = popupView.findViewById(R.id.btnDismissPopup);

        tvTitle.setText("Battery Low: " + percent + "%");
        tvMsg.setText("Please connect your charger soon to stay safe.");

        btnDismiss.setOnClickListener(v -> {
            try {
                windowManager.removeView(popupView);
            } catch (Exception ignored) {}
        });

        try {
            windowManager.addView(popupView, params);
        } catch (Exception e) {
            isShowing = false;
        }
    }
}
