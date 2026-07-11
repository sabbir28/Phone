package s28.system.phone;

import android.os.Build;
import android.telecom.Call;
import android.telecom.InCallService;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.M)
public class CallManager {
    private static CallManager instance;
    private final List<CallListener> listeners = new ArrayList<>();
    private final List<Call> calls = new ArrayList<>();
    private Call currentCall;
    private InCallService inCallService;

    public interface CallListener {
        void onCallAdded(Call call);
        void onCallRemoved(Call call);
        void onStateChanged(Call call, int state);
    }

    private CallManager() {}

    public static synchronized CallManager getInstance() {
        if (instance == null) {
            instance = new CallManager();
        }
        return instance;
    }

    public void addListener(CallListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(CallListener listener) {
        listeners.remove(listener);
    }

    public void updateCall(Call call) {
        if (call == null) return;
        
        if (!calls.contains(call)) {
            calls.add(call);
            this.currentCall = call;
            
            for (CallListener listener : new ArrayList<>(listeners)) {
                listener.onCallAdded(call);
            }
            
            call.registerCallback(new Call.Callback() {
                @Override
                public void onStateChanged(Call call, int state) {
                    for (CallListener listener : new ArrayList<>(listeners)) {
                        listener.onStateChanged(call, state);
                    }
                }
            });
        }
    }

    public void removeCall(Call call) {
        calls.remove(call);
        if (this.currentCall == call) {
            this.currentCall = calls.isEmpty() ? null : calls.get(calls.size() - 1);
        }
        for (CallListener listener : new ArrayList<>(listeners)) {
            listener.onCallRemoved(call);
        }
    }

    public void setInCallService(InCallService service) {
        this.inCallService = service;
    }

    public void setMute(boolean muted) {
        if (inCallService != null) {
            inCallService.setMuted(muted);
        }
    }

    public void setAudioRoute(int route) {
        if (inCallService != null) {
            inCallService.setAudioRoute(route);
        }
    }

    public Call getCurrentCall() {
        return currentCall;
    }
    
    public List<Call> getCalls() {
        return new ArrayList<>(calls);
    }

    public void answer() {
        if (currentCall != null) {
            currentCall.answer(currentCall.getDetails().getVideoState());
        }
    }

    public void disconnect() {
        if (currentCall != null) {
            currentCall.disconnect();
        }
    }

    public void hold() {
        if (currentCall != null) {
            if (currentCall.getState() == Call.STATE_ACTIVE) {
                currentCall.hold();
            } else if (currentCall.getState() == Call.STATE_HOLDING) {
                currentCall.unhold();
            }
        }
    }

    public void merge() {
        if (calls.size() >= 2) {
            Call firstCall = calls.get(0);
            Call secondCall = calls.get(1);
            
            // In Android Telecom, you typically call conference() on the Call object
            // or the system handles it if the calls are compatible.
            if (firstCall.getDetails().can(android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
                firstCall.conference(secondCall);
            }
        }
    }
    
    public void swap() {
        if (calls.size() >= 2) {
            for (Call call : calls) {
                if (call.getState() == Call.STATE_ACTIVE) {
                    call.hold();
                } else if (call.getState() == Call.STATE_HOLDING) {
                    call.unhold();
                    currentCall = call;
                }
            }
        }
    }
}
