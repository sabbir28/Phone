package s28.system.phone.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import java.util.ArrayList;
import java.util.List;
import s28.system.phone.models.Contact;

public class ContactRepository {
    private final Context context;

    public ContactRepository(Context context) {
        this.context = context;
    }

    public List<Contact> getAllContacts() {
        List<Contact> contacts = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        
        String[] projection = new String[]{
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        };

        Cursor cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            try {
                int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idIndex);
                    String name = cursor.getString(nameIndex);
                    String number = cursor.getString(numberIndex);
                    String photoUriStr = cursor.getString(photoIndex);
                    Uri photoUri = photoUriStr != null ? Uri.parse(photoUriStr) : null;
                    
                    contacts.add(new Contact(id, name, number, photoUri));
                }
            } finally {
                cursor.close();
            }
        }
        return contacts;
    }
}
