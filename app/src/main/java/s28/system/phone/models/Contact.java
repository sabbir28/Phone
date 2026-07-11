package s28.system.phone.models;

import android.net.Uri;

public class Contact {
    private final long id;
    private final String name;
    private final String phoneNumber;
    private final Uri photoUri;

    public Contact(long id, String name, String phoneNumber, Uri photoUri) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.photoUri = photoUri;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public Uri getPhotoUri() { return photoUri; }
}
