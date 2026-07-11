package s28.system.phone.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import java.util.ArrayList;
import java.util.List;
import s28.system.phone.models.CallLogItem;

public class CallLogRepository {
    private final Context context;

    public CallLogRepository(Context context) {
        this.context = context;
    }

    public List<CallLogItem> getCallLogs() {
        List<CallLogItem> logs = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        String[] projection = new String[]{
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        };

        Cursor cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        );

        if (cursor != null) {
            try {
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);

                while (cursor.moveToNext()) {
                    String number = cursor.getString(numberIndex);
                    String name = cursor.getString(nameIndex);
                    int type = cursor.getInt(typeIndex);
                    long date = cursor.getLong(dateIndex);
                    long duration = cursor.getLong(durationIndex);

                    logs.add(new CallLogItem(number, name, type, date, duration));
                }
            } finally {
                cursor.close();
            }
        }
        return logs;
    }
}
