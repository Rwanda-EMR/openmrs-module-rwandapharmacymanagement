package org.openmrs.module.rwandapharmacymanagement.integration.hie;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.integration.RetryOutcome;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipCallException;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipCallOutcome;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipInsuranceApiClient;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipInsuranceConfig;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HieRetryService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private AdministrationService administrationService;
    private IntegrationLogService integrationLogService;
    private HieMedicationApiClient hieMedicationApiClient;
    private RhipInsuranceConfig rhipInsuranceConfig;
    private RhipInsuranceApiClient rhipInsuranceApiClient;

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    public void setIntegrationLogService(IntegrationLogService integrationLogService) {
        this.integrationLogService = integrationLogService;
    }

    public void setHieMedicationApiClient(HieMedicationApiClient hieMedicationApiClient) {
        this.hieMedicationApiClient = hieMedicationApiClient;
    }

    public void setRhipInsuranceConfig(RhipInsuranceConfig rhipInsuranceConfig) {
        this.rhipInsuranceConfig = rhipInsuranceConfig;
    }

    public void setRhipInsuranceApiClient(RhipInsuranceApiClient rhipInsuranceApiClient) {
        this.rhipInsuranceApiClient = rhipInsuranceApiClient;
    }

    public int retryPending() {
        return retryPending(batchSize());
    }

    /**
     * Sends queued HIE payloads oldest first. A prescription's medication requests wait while its encounter is
     * still unsent, and the run stops at the first connection failure so an HIE outage does not stall the task.
     */
    public int retryPending(int maxResults) {
        if (!isHieEnabled() && !isRhipEnabled()) {
            return 0;
        }
        List<IntegrationLog> pendingLogs = integrationLogService.getPendingHieLogs(maxResults);
        int successful = 0;
        Set<Integer> unsentEncounterReferences = new HashSet<Integer>();
        for (IntegrationLog pendingLog : pendingLogs) {
            Integer referenceId = pendingLog.getReferenceId();
            if (pendingLog.getIntegrationType() == IntegrationType.HIE_MEDICATION_REQUEST && referenceId != null
                    && unsentEncounterReferences.contains(referenceId)) {
                continue;
            }
            RetryOutcome outcome = retry(pendingLog);
            if (outcome == RetryOutcome.SENT) {
                successful++;
                continue;
            }
            if (pendingLog.getIntegrationType() == IntegrationType.HIE_ENCOUNTER && referenceId != null) {
                unsentEncounterReferences.add(referenceId);
            }
            if (outcome == RetryOutcome.UNREACHABLE) {
                break;
            }
        }
        return successful;
    }

    public boolean retryLog(Integer integrationLogId) {
        if (!isHieEnabled() && !isRhipEnabled()) {
            return false;
        }
        IntegrationLog pendingLog = integrationLogService.getLog(integrationLogId);
        if (pendingLog == null || pendingLog.getStatus() != IntegrationStatus.RETRY_PENDING || !isRetriableType(pendingLog.getIntegrationType())) {
            return false;
        }
        if (!integrationEnabled(pendingLog.getIntegrationType())) {
            return false;
        }
        return retry(pendingLog) == RetryOutcome.SENT;
    }

    private RetryOutcome retry(IntegrationLog pendingLog) {
        if (!integrationEnabled(pendingLog == null ? null : pendingLog.getIntegrationType())) {
            return RetryOutcome.FAILED;
        }
        long start = System.currentTimeMillis();
        pendingLog.setRetryCount(Integer.valueOf((pendingLog.getRetryCount() == null ? 0 : pendingLog.getRetryCount().intValue()) + 1));
        pendingLog.setLastAttemptDate(new Date());
        try {
            String response = isRhipType(pendingLog.getIntegrationType())
                    ? rhipInsuranceApiClient.post(endpointPath(pendingLog.getIntegrationType()),
                            rhipInsuranceConfig.apiKey(),
                            rhipInsuranceConfig.apiOrigin(),
                            pendingLog.getRequestPayload(),
                            rhipInsuranceConfig.connectionTimeout(),
                            rhipInsuranceConfig.readTimeout())
                    : hieMedicationApiClient.post(
                            administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_BASE_URL, ""),
                            endpointPath(pendingLog.getIntegrationType()),
                            administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_USERNAME, ""),
                            administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_PASSWORD, ""),
                            pendingLog.getRequestPayload(),
                            integerProperty(RwandaPharmacyManagementConfig.GP_HIE_CONNECT_TIMEOUT, 10000),
                            integerProperty(RwandaPharmacyManagementConfig.GP_HIE_READ_TIMEOUT, 30000));
            pendingLog.setStatus(IntegrationStatus.SUCCESS);
            pendingLog.setResponsePayload(truncate(response));
            pendingLog.setSummary(isRhipType(pendingLog.getIntegrationType())
                    ? RhipCallOutcome.successSummary(pendingLog.getIntegrationType(), response)
                    : "Queued HIE payload sent successfully");
            pendingLog.setDurationMs(Long.valueOf(Math.max(0L, System.currentTimeMillis() - start)));
            integrationLogService.saveLog(pendingLog);
            return RetryOutcome.SENT;
        }
        catch (Exception e) {
            log.error("Queued integration payload retry failed. logId=" + pendingLog.getIntegrationLogId()
                    + ", type=" + pendingLog.getIntegrationType(), e);
            boolean unreachable = RetryOutcome.isUnreachable(e);
            if (isRhipType(pendingLog.getIntegrationType())) {
                // Only calls that never reached RHIP, or got 429/502/503/504, stay in the queue
                pendingLog.setStatus(RhipCallOutcome.failureStatus(e));
                pendingLog.setResponsePayload(truncate(e instanceof RhipCallException
                        && ((RhipCallException) e).getResponseBody() != null
                        ? ((RhipCallException) e).getResponseBody() : e.getMessage()));
                pendingLog.setSummary(RhipCallOutcome.failureSummary(pendingLog.getIntegrationType(), e));
            }
            else {
                boolean giveUp = !unreachable && pendingLog.getRetryCount().intValue() >= maxRetryAttempts();
                pendingLog.setStatus(giveUp ? IntegrationStatus.FAILED : IntegrationStatus.RETRY_PENDING);
                pendingLog.setResponsePayload(truncate(e.getMessage()));
                pendingLog.setSummary(giveUp
                        ? "HIE rejected the payload; stopped after " + pendingLog.getRetryCount() + " attempts"
                        : unreachable ? "HIE unreachable; will retry" : "Queued HIE payload retry failed");
            }
            pendingLog.setDurationMs(Long.valueOf(Math.max(0L, System.currentTimeMillis() - start)));
            integrationLogService.saveLog(pendingLog);
            return unreachable ? RetryOutcome.UNREACHABLE : RetryOutcome.FAILED;
        }
    }

    private int maxRetryAttempts() {
        return Math.max(1, integerProperty(RwandaPharmacyManagementConfig.GP_INTEGRATION_MAX_RETRY_ATTEMPTS, 12).intValue());
    }

    private String endpointPath(IntegrationType integrationType) {
        if (integrationType == IntegrationType.HIE_ENCOUNTER) {
            return administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_PATH, "/Encounter");
        }
        if (integrationType == IntegrationType.HIE_MEDICATION_DISPENSE) {
            return administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_DISPENSE_PATH, "/MedicationDispense");
        }
        if (integrationType == IntegrationType.RHIP_INPATIENT_PRESCRIPTION) {
            return rhipInsuranceConfig == null ? "" : rhipInsuranceConfig.inpatientPrescriptionUrl();
        }
        if (integrationType == IntegrationType.RHIP_VISIT_DISPENSE) {
            return rhipInsuranceConfig == null ? "" : rhipInsuranceConfig.visitDispenseUrl();
        }
        return administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_REQUEST_PATH, "/MedicationRequest");
    }

    private boolean isHieEnabled() {
        return Boolean.valueOf(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENABLED, "false"))
                && !isBlank(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_BASE_URL, ""));
    }

    private boolean isRhipEnabled() {
        return rhipInsuranceConfig != null && rhipInsuranceApiClient != null && rhipInsuranceConfig.isEnabled();
    }

    private boolean isRetriableType(IntegrationType integrationType) {
        return isHieType(integrationType) || isRhipType(integrationType);
    }

    private boolean integrationEnabled(IntegrationType integrationType) {
        if (isRhipType(integrationType)) {
            return isRhipEnabled();
        }
        if (isHieType(integrationType)) {
            return isHieEnabled();
        }
        return false;
    }

    private boolean isHieType(IntegrationType integrationType) {
        return integrationType == IntegrationType.HIE_ENCOUNTER
                || integrationType == IntegrationType.HIE_MEDICATION_REQUEST
                || integrationType == IntegrationType.HIE_MEDICATION_DISPENSE;
    }

    private boolean isRhipType(IntegrationType integrationType) {
        return integrationType == IntegrationType.RHIP_INPATIENT_PRESCRIPTION
                || integrationType == IntegrationType.RHIP_VISIT_DISPENSE;
    }

    private int batchSize() {
        return integerProperty(RwandaPharmacyManagementConfig.GP_HIE_RETRY_BATCH_SIZE, 50);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
