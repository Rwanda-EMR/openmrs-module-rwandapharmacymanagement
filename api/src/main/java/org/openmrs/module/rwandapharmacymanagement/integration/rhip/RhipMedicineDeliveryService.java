package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.integration.rhip.RhipInsurancePayloadBuilder.RhipPrescriptionPayload;
import org.openmrs.module.rwandapharmacymanagement.model.DeliveryReceipt;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Records delivery receipts with MMI (RHIP /practitioner/medicines/delivery) so that in-house medicines are billed at
 * the delivery price. The receipt number is the delivery reference that medicine lines on the voucher point to.
 * A delivery is never sent again automatically: sending the same delivery twice could count its stock twice at MMI,
 * so a delivery that failed is resent from the MMI deliveries page.
 */
public class RhipMedicineDeliveryService {

    private final Log log = LogFactory.getLog(getClass());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RhipInsuranceConfig rhipInsuranceConfig;

    private RhipInsuranceApiClient rhipInsuranceApiClient;

    private RhipInsurancePayloadBuilder rhipInsurancePayloadBuilder;

    private IntegrationLogService integrationLogService;

    private MohBillingRhipIntegrationLogBridge mohBillingRhipIntegrationLogBridge;

    public void setRhipInsuranceConfig(RhipInsuranceConfig rhipInsuranceConfig) {
        this.rhipInsuranceConfig = rhipInsuranceConfig;
    }

    public void setRhipInsuranceApiClient(RhipInsuranceApiClient rhipInsuranceApiClient) {
        this.rhipInsuranceApiClient = rhipInsuranceApiClient;
    }

    public void setRhipInsurancePayloadBuilder(RhipInsurancePayloadBuilder rhipInsurancePayloadBuilder) {
        this.rhipInsurancePayloadBuilder = rhipInsurancePayloadBuilder;
    }

    public void setIntegrationLogService(IntegrationLogService integrationLogService) {
        this.integrationLogService = integrationLogService;
    }

    public void setMohBillingRhipIntegrationLogBridge(MohBillingRhipIntegrationLogBridge mohBillingRhipIntegrationLogBridge) {
        this.mohBillingRhipIntegrationLogBridge = mohBillingRhipIntegrationLogBridge;
    }

    public boolean isEnabled() {
        return rhipInsuranceConfig != null && rhipInsuranceConfig.isMedicineDeliveryEnabled();
    }

    /**
     * Sends the receipt once the transaction that received it has committed, so a receipt that is rolled back is
     * never recorded at MMI. Failures never reach the caller: receiving stock must not fail because RHIP is down.
     */
    public void sendAfterCommit(final DeliveryReceipt receipt) {
        if (!isEnabled() || receipt == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendQuietly(receipt);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                sendQuietly(receipt);
            }
        });
    }

    private void sendQuietly(DeliveryReceipt receipt) {
        try {
            send(receipt);
        }
        catch (Exception e) {
            log.error("Unable to send delivery receipt " + receipt.getDeliveryReceiptId() + " to RHIP", e);
        }
    }

    /**
     * Sends the receipt to MMI now and records the outcome
     * @return the integration log of this attempt
     */
    public IntegrationLog send(DeliveryReceipt receipt) {
        String endpoint = rhipInsuranceConfig == null ? "" : rhipInsuranceConfig.medicineDeliveryUrl();
        String reference = RhipInsurancePayloadBuilder.deliveryReferenceNumber(receipt);
        if (!isEnabled()) {
            return saveLog(IntegrationStatus.SKIPPED, endpoint, referenceStub(reference), "",
                    "MMI medicine delivery integration is disabled", System.currentTimeMillis());
        }
        long start = System.currentTimeMillis();
        String payload = referenceStub(reference);
        try {
            RhipPrescriptionPayload prepared = rhipInsurancePayloadBuilder.buildMedicineDelivery(receipt);
            if (!prepared.isReady()) {
                return saveLog(IntegrationStatus.SKIPPED, endpoint, payload, "", prepared.getSkipReason(), start);
            }
            payload = prepared.getPayload();
            String response = rhipInsuranceApiClient.post(endpoint, rhipInsuranceConfig.apiKey(),
                    rhipInsuranceConfig.apiOrigin(), payload, rhipInsuranceConfig.connectionTimeout(),
                    rhipInsuranceConfig.readTimeout());
            String summary = RhipCallOutcome.successSummary(IntegrationType.RHIP_MEDICINE_DELIVERY, response);
            if (!prepared.getNotSentLines().isEmpty()) {
                summary += "; not sent: " + join(prepared.getNotSentLines());
            }
            return saveLog(IntegrationStatus.SUCCESS, endpoint, payload, response, summary, start);
        }
        catch (Exception e) {
            log.error("RHIP medicine delivery failed. reference=" + reference, e);
            String responseBody = e instanceof RhipCallException && ((RhipCallException) e).getResponseBody() != null
                    ? ((RhipCallException) e).getResponseBody() : e.getMessage();
            String summary = RhipCallOutcome.failureSummary(IntegrationType.RHIP_MEDICINE_DELIVERY, e);
            return saveLog(IntegrationStatus.FAILED, endpoint, payload, responseBody, summary, start,
                    RhipCallOutcome.httpStatus(e));
        }
    }

    /**
     * @return the latest MMI delivery log for each delivery reference, among logs created on or after the date
     */
    public Map<String, IntegrationLog> getLatestLogsByReference(Date fromInclusive) {
        Map<String, IntegrationLog> ret = new LinkedHashMap<String, IntegrationLog>();
        if (integrationLogService == null) {
            return ret;
        }
        List<IntegrationLog> logs = integrationLogService.getLogs(IntegrationType.RHIP_MEDICINE_DELIVERY, fromInclusive);
        if (logs == null) {
            return ret;
        }
        for (IntegrationLog integrationLog : logs) {
            String reference = referenceNumber(integrationLog == null ? null : integrationLog.getRequestPayload());
            if (reference != null && !ret.containsKey(reference)) {
                ret.put(reference, integrationLog);
            }
        }
        return ret;
    }

    static String referenceNumber(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode value = OBJECT_MAPPER.readTree(payload).get("referenceNumber");
            return value == null || value.isNull() ? null : value.asText();
        }
        catch (Exception e) {
            return null;
        }
    }

    private String referenceStub(String reference) {
        try {
            Map<String, Object> stub = new LinkedHashMap<String, Object>();
            stub.put("referenceNumber", reference);
            return OBJECT_MAPPER.writeValueAsString(stub);
        }
        catch (Exception e) {
            return "";
        }
    }

    private IntegrationLog saveLog(IntegrationStatus status, String endpoint, String payload, String response,
                                   String summary, long start) {
        return saveLog(status, endpoint, payload, response, summary, start, null);
    }

    private IntegrationLog saveLog(IntegrationStatus status, String endpoint, String payload, String response,
                                   String summary, long start, Integer httpStatus) {
        IntegrationLog integrationLog = new IntegrationLog();
        integrationLog.setDateCreated(new Date());
        integrationLog.setIntegrationType(IntegrationType.RHIP_MEDICINE_DELIVERY);
        integrationLog.setStatus(status);
        integrationLog.setEndpoint(endpoint);
        integrationLog.setRequestPayload(truncate(payload));
        integrationLog.setResponsePayload(truncate(response));
        integrationLog.setSummary(truncate(summary));
        integrationLog.setDurationMs(Math.max(0L, System.currentTimeMillis() - start));
        integrationLog.setLastAttemptDate(new Date());
        integrationLog.setCreator(Context.getAuthenticatedUser());
        IntegrationLog saved = integrationLogService == null ? integrationLog : integrationLogService.saveLog(integrationLog);
        if (mohBillingRhipIntegrationLogBridge != null && status != IntegrationStatus.SKIPPED) {
            mohBillingRhipIntegrationLogBridge.save(IntegrationType.RHIP_MEDICINE_DELIVERY, status, endpoint,
                    truncate(payload), truncate(response), summary, httpStatus);
        }
        return saved;
    }

    private String join(List<String> values) {
        StringBuilder ret = new StringBuilder();
        for (String value : values) {
            if (ret.length() > 0) {
                ret.append(", ");
            }
            ret.append(value);
        }
        return ret.toString();
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 4000 ? value : value.substring(0, 4000);
    }
}
