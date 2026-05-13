package org.openmrs.module.rwandapharmacymanagement.util;

import org.openmrs.PersonName;
import org.openmrs.User;

public final class UserNameUtils {

    private UserNameUtils() {
    }

    public static String plainName(User user) {
        if (user == null) {
            return "";
        }

        PersonName personName = user.getPersonName();
        if (personName != null) {
            String fullName = trim(personName.getFullName());
            if (!fullName.isEmpty()) {
                return fullName;
            }

            String givenName = trim(personName.getGivenName());
            String familyName = trim(personName.getFamilyName());
            String combined = (givenName + " " + familyName).trim();
            if (!combined.isEmpty()) {
                return combined;
            }
        }

        return trim(user.getUsername());
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
