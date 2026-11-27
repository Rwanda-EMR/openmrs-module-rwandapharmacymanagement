package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import java.io.IOException;

/**
 * A failed call to RHIP, telling apart a call that never reached RHIP, one that got no reply, and one RHIP answered
 * with an error status. Only some of these may be sent again (RHIP Unified Insurance Integration Guide: MMI, section 15).
 */
public class RhipCallException extends IOException {

    private static final long serialVersionUID = 1L;

    private final boolean sent;

    private final Integer statusCode;

    private final String responseBody;

    private final String retryAfter;

    private RhipCallException(String message, Throwable cause, boolean sent, Integer statusCode, String responseBody,
                              String retryAfter) {
        super(message, cause);
        this.sent = sent;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.retryAfter = retryAfter;
    }

    public static RhipCallException notSent(IOException cause) {
        return new RhipCallException("Could not reach RHIP: " + cause.getMessage(), cause, false, null, null, null);
    }

    public static RhipCallException noReply(IOException cause) {
        return new RhipCallException("No reply from RHIP: " + cause.getMessage(), cause, true, null, null, null);
    }

    public static RhipCallException httpError(int statusCode, String responseBody, String retryAfter) {
        return new RhipCallException("RHIP API call failed with HTTP " + statusCode + ": " + responseBody, null, true,
                Integer.valueOf(statusCode), responseBody, retryAfter);
    }

    /**
     * @return false when the request never reached RHIP
     */
    public boolean isSent() {
        return sent;
    }

    /**
     * @return the HTTP status RHIP answered with, or null when there was no answer
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getRetryAfter() {
        return retryAfter;
    }

    /**
     * @return true when sending the same request again is safe and may succeed: it never reached RHIP, RHIP asked to
     * slow down (429), or MMI was unreachable (502, 503, 504)
     */
    public boolean isRetriable() {
        if (!sent) {
            return true;
        }
        if (statusCode == null) {
            return false;
        }
        int code = statusCode.intValue();
        return code == 429 || code == 502 || code == 503 || code == 504;
    }
}
