package s28.system.phone;

import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.InCallService;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class DialerInCallService extends InCallService {

    @Override
    public void onCreate() {
        super.onCreate();
        CallManager.getInstance().setInCallService(this);
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        CallManager.getInstance().updateCall(call);
        
        Intent intent = new Intent(this, InCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
