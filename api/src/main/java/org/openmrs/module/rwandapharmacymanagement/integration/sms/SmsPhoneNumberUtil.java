package org.openmrs.module.rwandapharmacymanagement.integration.sms;

public final class SmsPhoneNumberUtil {

    private SmsPhoneNumberUtil() {
    }

    public static String normalizeForSending(String phoneNumber) {
        String digits = digitsOnly(phoneNumber);
        if (digits.length() == 12 && digits.startsWith("25")) {
            return digits;
        }
        if (digits.length() == 10) {
            return "25" + digits;
        }
        return null;
    }

    public static boolean isValidForSending(String phoneNumber) {
        return normalizeForSending(phoneNumber) != null;
    }

    private static String digitsOnly(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < phoneNumber.length(); i++) {
            char current = phoneNumber.charAt(i);
            if (Character.isDigit(current)) {
                digits.append(current);
            }
        }
        return digits.toString();
    }
}
