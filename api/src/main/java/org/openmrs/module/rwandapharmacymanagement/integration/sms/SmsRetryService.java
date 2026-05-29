package org.openmrs.module.rwandapharmacymanagement.integration.sms;

import org.openmrs.api.AdministrationService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class SmsRetryService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private AdministrationService administrationService;
    private IntegrationLogService integrationLogService;
    private SmsGatewayClient smsGatewayClient;

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    public void setIntegrationLogService(IntegrationLogService integrationLogService) {
        this.integrationLogService = integrationLogService;
    }

    public void setSmsGatewayClient(SmsGatewayClient smsGatewayClient) {
        this.smsGatewayClient = smsGatewayClient;
    }

    public int retryPending() {
        return retryPending(batchSize());
    }

    public int retryPending(int maxResults) {
        if (!isSmsEnabled()) {
            return 0;
        }
        List<IntegrationLog> pendingLogs = integrationLogService.getPendingSmsLogs(maxResults);
        int successful = 0;
        for (IntegrationLog pendingLog : pendingLogs) {
            if (retry(pendingLog)) {
                successful++;
            }
        }
        return successful;
    }

    public boolean retryLog(Integer integrationLogId) {
        if (!isSmsEnabled()) {
            return false;
        }
        IntegrationLog pendingLog = integrationLogService.getLog(integrationLogId);
        if (pendingLog == null || pendingLog.getStatus() != IntegrationStatus.RETRY_PENDING
                || pendingLog.getIntegrationType() != IntegrationType.SMS_NOTIFICATION) {
            return false;
        }
        return retry(pendingLog);
    }

    private boolean retry(IntegrationLog pendingLog) {
        long start = System.currentTimeMillis();
        pendingLog.setRetryCount(Integer.valueOf((pendingLog.getRetryCount() == null ? 0 : pendingLog.getRetryCount().intValue()) + 1));
        pendingLog.setLastAttemptDate(new Date());
        try {
            String response = smsGatewayClient.retry(pendingLog.getRequestPayload(),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_USERNAME, ""),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_PASSWORD, ""),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SMS_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SMS_READ_TIMEOUT, 30000));
            pendingLog.setStatus(IntegrationStatus.SUCCESS);
            pendingLog.setResponsePayload(truncate(response));
            pendingLog.setSummary("Queued prescription SMS sent successfully");
            pendingLog.setDurationMs(Long.valueOf(Math.max(0L, System.currentTimeMillis() - start)));
            integrationLogService.saveLog(pendingLog);
            return true;
        }
        catch (Exception e) {
            log.error("Queued prescription SMS retry failed. logId=" + pendingLog.getIntegrationLogId(), e);
            pendingLog.setStatus(IntegrationStatus.RETRY_PENDING);
            pendingLog.setResponsePayload(truncate(e.getMessage()));
            pendingLog.setSummary("Queued prescription SMS retry failed");
            pendingLog.setDurationMs(Long.valueOf(Math.max(0L, System.currentTimeMillis() - start)));
            integrationLogService.saveLog(pendingLog);
            return false;
        }
    }

    private boolean isSmsEnabled() {
        return Boolean.valueOf(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_ENABLED, "false"));
    }

    private int batchSize() {
        return integerProperty(RwandaPharmacyManagementConfig.GP_SMS_RETRY_BATCH_SIZE, 50);
    }

    private Integer integerProperty(String property, int defaultValue) {
        try {
            return Integer.valueOf(administrationService.getGlobalProperty(property, String.valueOf(defaultValue)));
        }
        catch (Exception ignored) {
            return Integer.valueOf(defaultValue);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 16000000 ? value.substring(0, 16000000) : value;
    }
}
