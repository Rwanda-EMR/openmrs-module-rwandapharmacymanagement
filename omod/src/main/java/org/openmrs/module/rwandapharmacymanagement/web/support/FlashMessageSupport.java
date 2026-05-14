package org.openmrs.module.rwandapharmacymanagement.web.support;

import javax.servlet.http.HttpSession;

public final class FlashMessageSupport {

    private FlashMessageSupport() {
    }

    public static void set(HttpSession session, String key, Object value) {
        if (session != null && key != null) {
            session.setAttribute(key, value);
        }
    }

    public static boolean consumeBoolean(HttpSession session, String key) {
        Object value = consume(session, key);
        return Boolean.TRUE.equals(value);
    }

    public static String consumeString(HttpSession session, String key) {
        Object value = consume(session, key);
        return value == null ? null : String.valueOf(value);
    }

    public static Integer consumeInteger(HttpSession session, String key) {
        Object value = consume(session, key);
        return value instanceof Integer ? (Integer) value : null;
    }

    public static Object consume(HttpSession session, String key) {
        if (session == null || key == null) {
            return null;
        }
        Object value = session.getAttribute(key);
        session.removeAttribute(key);
        return value;
    }
}
