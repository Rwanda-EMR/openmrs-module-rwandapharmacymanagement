package org.openmrs.module.rwandapharmacymanagement.integration.sms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SmsPhoneNumberUtilTest {

    @Test
    public void normalizeForSendingShouldKeepValidTwelveDigitNumber() {
        assertEquals("250788453692", SmsPhoneNumberUtil.normalizeForSending("250788453692"));
    }

    @Test
    public void normalizeForSendingShouldPrefixTenDigitNumber() {
        assertEquals("250788453692", SmsPhoneNumberUtil.normalizeForSending("0788453692"));
    }

    @Test
    public void normalizeForSendingShouldRejectInvalidNumber() {
        assertNull(SmsPhoneNumberUtil.normalizeForSending("788453692"));
    }

    @Test
    public void isValidForSendingShouldReflectNormalization() {
        assertTrue(SmsPhoneNumberUtil.isValidForSending("0788453692"));
        assertFalse(SmsPhoneNumberUtil.isValidForSending(""));
    }
}
