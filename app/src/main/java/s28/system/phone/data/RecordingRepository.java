package s28.system.phone.data;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import s28.system.phone.models.RecordingItem;

public class RecordingRepository {
    private final Context context;

    public RecordingRepository(Context context) {
        this.context = context;
    }

    public List<RecordingItem> getRecordingsForNumber(String phoneNumber) {
        List<RecordingItem> recordings = new ArrayList<>();
        File recordDir = new File(context.getExternalFilesDir(null), "Recordings");
        
        if (!recordDir.exists()) return recordings;

        String cleanSearchNumber = (phoneNumber != null) ? phoneNumber.replaceAll("[^0-9]", "") : "";
        
        File[] files = recordDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if ((name.startsWith("Recording_") || name.startsWith("SystemRecording_") || name.startsWith("GoogleRecording_")) && name.endsWith(".m4a")) {
                    // Format: [Prefix]_[Number]_[Timestamp].m4a
                    String[] parts = name.split("_");
                    if (parts.length >= 3) {
                        String fileNumber = parts[1];
                        if (fileNumber.equals(cleanSearchNumber) || cleanSearchNumber.isEmpty()) {
                            recordings.add(new RecordingItem(
                                file.getAbsolutePath(),
                                name,
                                file.lastModified(),
                                fileNumber
                            ));
                        }
                    }
                }
            }
        }
        
        // Sort by timestamp descending
        Collections.sort(recordings, new Comparator<RecordingItem>() {
            @Override
            public int compare(RecordingItem a, RecordingItem b) {
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            }
        });
        
        return recordings;
    }
}
