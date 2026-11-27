package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.UUID;

public class MohBillingRhipIntegrationLogBridge {

    private final Log log = LogFactory.getLog(getClass());

    public void save(IntegrationType type, IntegrationStatus status, String endpoint, String requestPayload,
                     String responsePayload, String summary) {
        save(type, status, endpoint, requestPayload, responsePayload, summary, null);
    }

    /**
     * @param httpStatus the HTTP status RHIP answered with, or null when there was no answer (200 is assumed on success)
     */
    public void save(IntegrationType type, IntegrationStatus status, String endpoint, String requestPayload,
                     String responsePayload, String summary, Integer httpStatus) {
        if (!isRhipType(type)) {
            return;
        }
        try {
            Class<?> billingServiceClass = Context.loadClass("org.openmrs.module.mohbilling.service.BillingService");
            Object billingService = Context.getService(billingServiceClass);
            if (billingService == null) {
                return;
            }
            Class<?> logClass = Context.loadClass("org.openmrs.module.mohbilling.model.RhipIntegrationLog");
            Object logEntry = logClass.newInstance();
            User currentUser = currentUser();

            set(logClass, logEntry, "setDateCreated", Date.class, new Date());
            set(logClass, logEntry, "setCreator", User.class, currentUser);
            set(logClass, logEntry, "setSenderUsername", String.class, currentUser == null ? null : currentUser.getUsername());
            set(logClass, logEntry, "setOperationType", String.class, operationType(type));
            set(logClass, logEntry, "setEndpointUrl", String.class, endpoint);
            set(logClass, logEntry, "setRequestPayload", String.class, requestPayload);
            set(logClass, logEntry, "setResponseCode", Integer.class, responseCode(status, httpStatus));
            set(logClass, logEntry, "setResponseStatus", String.class, responseStatus(status));
            set(logClass, logEntry, "setResponseBody", String.class, responsePayload);
            set(logClass, logEntry, "setErrorMessage", String.class, status == IntegrationStatus.SUCCESS ? null : summary);
            set(logClass, logEntry, "setUuid", String.class, UUID.randomUUID().toString());

            Method saveMethod = billingServiceClass.getMethod("saveRhipIntegrationLog", logClass);
            saveMethod.invoke(billingService, logEntry);
        }
        catch (Exception e) {
            log.warn("Unable to persist RHIP integration log entry in MoH Billing", e);
        }
    }

    private boolean isRhipType(IntegrationType type) {
        return type == IntegrationType.RHIP_INPATIENT_PRESCRIPTION || type == IntegrationType.RHIP_VISIT_DISPENSE
                || type == IntegrationType.RHIP_MEDICINE_DELIVERY;
    }

    private String operationType(IntegrationType type) {
        if (type == IntegrationType.RHIP_INPATIENT_PRESCRIPTION) {
            return "INPATIENT_PRESCRIPTION";
        }
        if (type == IntegrationType.RHIP_VISIT_DISPENSE) {
            return "VISIT_DISPENSE";
        }
        if (type == IntegrationType.RHIP_MEDICINE_DELIVERY) {
            return "MEDICINE_DELIVERY";
        }
        return type == null ? null : type.name();
    }

    private Integer responseCode(IntegrationStatus status, Integer httpStatus) {
        if (httpStatus != null) {
            return httpStatus;
        }
        return status == IntegrationStatus.SUCCESS ? Integer.valueOf(200) : null;
    }

    private String responseStatus(IntegrationStatus status) {
        if (status == IntegrationStatus.SUCCESS) {
            return "SUCCESS";
        }
        if (status == IntegrationStatus.SKIPPED) {
            return "SKIPPED";
        }
        if (status == IntegrationStatus.RETRY_PENDING || status == IntegrationStatus.FAILED) {
            return "ERROR";
        }
        return "UNKNOWN";
    }

    private User currentUser() {
        try {
            return Context.getAuthenticatedUser();
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private void set(Class<?> logClass, Object logEntry, String methodName, Class<?> parameterType, Object value)
            throws Exception {
        logClass.getMethod(methodName, parameterType).invoke(logEntry, value);
    }
}
