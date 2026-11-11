package org.openmrs.module.rwandapharmacymanagement.integration;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Result of sending one queued integration payload.
 */
public enum RetryOutcome {
    SENT,
    FAILED,
    UNREACHABLE;

    /**
     * True when the remote system could not be reached, did not answer in time, or answered with a 5xx server
     * error, as opposed to rejecting the payload. Such failures are retried without limit.
     */
    public static boolean isUnreachable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ConnectException || current instanceof NoRouteToHostException
                    || current instanceof UnknownHostException || current instanceof SocketTimeoutException) {
                return true;
            }
            // HieMedicationApiClient and SmsGatewayClient report HTTP errors as "... failed with HTTP <code>: ...".
            String message = current.getMessage();
            if (message != null && message.matches("(?s).*failed with HTTP 5\\d\\d.*")) {
                return true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }
}
