package s28.system.phone.models;

public class CallLogItem {
    private final String number;
    private final String name;
    private final int type;
    private final long date;
    private final long duration;

    public CallLogItem(String number, String name, int type, long date, long duration) {
        this.number = number;
        this.name = name;
        this.type = type;
        this.date = date;
        this.duration = duration;
    }

    public String getNumber() { return number; }
    public String getName() { return name; }
    public int getType() { return type; }
    public long getDate() { return date; }
    public long getDuration() { return duration; }
}
