package s28.system.phone.lib;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Locale;

public class PhoneNumberOfflineGeocoder {

    private static PhoneNumberOfflineGeocoder instance;
    private final PhoneNumberUtil phoneUtil;

    private PhoneNumberOfflineGeocoder() {
        phoneUtil = PhoneNumberUtil.getInstance();
    }

    public static synchronized PhoneNumberOfflineGeocoder getInstance() {
        if (instance == null) {
            instance = new PhoneNumberOfflineGeocoder();
        }
        return instance;
    }

    public String getDescriptionForNumber(Phonenumber.PhoneNumber phoneNumber, Locale locale) {
        if (phoneNumber == null) {
            return "";
        }
        
        String regionCode = phoneUtil.getRegionCodeForNumber(phoneNumber);
        
        // "ZZ" is the region code returned by PhoneNumberUtil when it can't determine the region.
        if (regionCode == null || regionCode.equals("ZZ")) {
            return "Unknown Location";
        }

        Locale regionLocale = new Locale("", regionCode);
        return regionLocale.getDisplayCountry(locale);
    }
}
