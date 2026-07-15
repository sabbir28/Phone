package s28.system.phone.models;

import java.io.File;

public class RecordingItem {
    private final String filePath;
    private final String fileName;
    private final long timestamp;
    private final String phoneNumber;

    public RecordingItem(String filePath, String fileName, long timestamp, String phoneNumber) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.phoneNumber = phoneNumber;
    }

    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public long getTimestamp() { return timestamp; }
    public String getPhoneNumber() { return phoneNumber; }
    
    public File getFile() {
        return new File(filePath);
    }
}
