package org.openmrs.module.rwandapharmacymanagement.integration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.openmrs.module.rwandapharmacymanagement.api.IntegrationLogService;
import org.openmrs.module.rwandapharmacymanagement.integration.hie.HieMedicationApiClient;
import org.openmrs.module.rwandapharmacymanagement.integration.hie.HieMedicationPayloadBuilder;
import org.openmrs.module.rwandapharmacymanagement.integration.sms.SmsGatewayClient;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecord;
import org.openmrs.module.rwandapharmacymanagement.model.DispenseRecordLine;
import org.openmrs.module.rwandapharmacymanagement.model.IntegrationLog;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequest;
import org.openmrs.module.rwandapharmacymanagement.model.PatientDispensingRequestLine;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationStatus;
import org.openmrs.module.rwandapharmacymanagement.model.enums.IntegrationType;
import org.openmrs.module.rwandapharmacymanagement.model.enums.RequestType;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrescriptionCommunicationService {

    private final Log log = LogFactory.getLog(getClass());

    private AdministrationService administrationService;
    private IntegrationLogService integrationLogService;
    private HieMedicationApiClient hieMedicationApiClient;
    private HieMedicationPayloadBuilder hieMedicationPayloadBuilder;
    private SmsGatewayClient smsGatewayClient;

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    public void setIntegrationLogService(IntegrationLogService integrationLogService) {
        this.integrationLogService = integrationLogService;
    }

    public void setHieMedicationApiClient(HieMedicationApiClient hieMedicationApiClient) {
        this.hieMedicationApiClient = hieMedicationApiClient;
    }

    public void setHieMedicationPayloadBuilder(HieMedicationPayloadBuilder hieMedicationPayloadBuilder) {
        this.hieMedicationPayloadBuilder = hieMedicationPayloadBuilder;
    }

    public void setSmsGatewayClient(SmsGatewayClient smsGatewayClient) {
        this.smsGatewayClient = smsGatewayClient;
    }

    public void handlePrescriptionSaved(PatientDispensingRequest request, boolean sendSms) {
        if (request == null || request.getRequestType() != RequestType.PRESCRIPTION) {
            return;
        }
        sendMedicationRequests(request, request.getLines());
        if (sendSms) {
            sendPrescriptionSavedSms(request);
        }
    }

    public void handlePrescriptionLineUpdated(PatientDispensingRequest request, PatientDispensingRequestLine requestLine) {
        if (request == null || requestLine == null || request.getRequestType() != RequestType.PRESCRIPTION) {
            return;
        }
        sendMedicationRequests(request, Collections.singletonList(requestLine));
    }

    public void handlePrescriptionDispensed(DispenseRecord dispenseRecord) {
        if (dispenseRecord == null || Boolean.TRUE.equals(dispenseRecord.getDispensedWithoutOrder())
                || dispenseRecord.getPatientDispensingRequest() == null
                || dispenseRecord.getPatientDispensingRequest().getRequestType() != RequestType.PRESCRIPTION) {
            return;
        }
        if (!isHieEnabled()) {
            log(IntegrationType.HIE_MEDICATION_DISPENSE, IntegrationStatus.SKIPPED,
                    buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_DISPENSE_PATH, "")),
                    "", "HIE medication dispense integration is disabled");
            return;
        }
        if (dispenseRecord.getLines() == null) {
            return;
        }
        for (DispenseRecordLine line : dispenseRecord.getLines()) {
            if (line == null) {
                continue;
            }
            long start = System.currentTimeMillis();
            String payload = "";
            try {
                payload = hieMedicationPayloadBuilder.buildMedicationDispense(dispenseRecord, line,
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_PATIENT_IDENTIFIER_TYPE_UUID, ""),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_CODE_SYSTEM, ""));
                String response = hieMedicationApiClient.post(
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_BASE_URL, ""),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_DISPENSE_PATH, "/MedicationDispense"),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_USERNAME, ""),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_PASSWORD, ""),
                        payload,
                        integerProperty(RwandaPharmacyManagementConfig.GP_HIE_CONNECT_TIMEOUT, 10000),
                        integerProperty(RwandaPharmacyManagementConfig.GP_HIE_READ_TIMEOUT, 30000));
                log.info("HIE MedicationDispense success. requestId="
                        + safeRequestId(dispenseRecord.getPatientDispensingRequest()) + ", lineId="
                        + line.getDispenseRecordLineId() + ", endpoint="
                        + buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_DISPENSE_PATH, "/MedicationDispense"))
                        + ", payload=" + truncate(payload) + ", response=" + truncate(response));
                log(IntegrationType.HIE_MEDICATION_DISPENSE, IntegrationStatus.SUCCESS,
                        buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_DISPENSE_PATH, "/MedicationDispense")),
                        payload, truncate(response), "Medication dispense sent to HIE", start);
            }
            catch (Exception e) {
                log.error("HIE MedicationDispense failed. requestId="
                        + safeRequestId(dispenseRecord.getPatientDispensingRequest()) + ", lineId="
                        + line.getDispenseRecordLineId() + ", endpoint="
                        + buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_DISPENSE_PATH, "/MedicationDispense"))
                        + ", payload=" + truncate(payload), e);
                log(IntegrationType.HIE_MEDICATION_DISPENSE, IntegrationStatus.FAILED,
                        buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_DISPENSE_PATH, "/MedicationDispense")),
                        payload, truncate(e.getMessage()), "Failed to send medication dispense to HIE", start);
            }
        }
    }

    private void sendMedicationRequests(PatientDispensingRequest request, List<PatientDispensingRequestLine> lines) {
        if (request == null || lines == null || lines.isEmpty()) {
            return;
        }
        if (!isHieEnabled()) {
            log(IntegrationType.HIE_MEDICATION_REQUEST, IntegrationStatus.SKIPPED,
                    buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_REQUEST_PATH, "")),
                    "", "HIE medication request integration is disabled");
            return;
        }
        if (!sendEncounter(request)) {
            return;
        }
        for (PatientDispensingRequestLine line : lines) {
            if (line == null) {
                continue;
            }
            long start = System.currentTimeMillis();
            String payload = "";
            try {
                payload = hieMedicationPayloadBuilder.buildMedicationRequest(request, line,
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_PATIENT_IDENTIFIER_TYPE_UUID, ""),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_CODE_SYSTEM, ""),
                        facilityId("unknown"),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_GROUP_IDENTIFIER_SYSTEM,
                                "http://openmrs.org/rwanda-pharmacy-management/prescription-order-number"));
                String response = hieMedicationApiClient.post(
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_BASE_URL, ""),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_REQUEST_PATH, "/MedicationRequest"),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_USERNAME, ""),
                        administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_PASSWORD, ""),
                        payload,
                        integerProperty(RwandaPharmacyManagementConfig.GP_HIE_CONNECT_TIMEOUT, 10000),
                        integerProperty(RwandaPharmacyManagementConfig.GP_HIE_READ_TIMEOUT, 30000));
                log.info("HIE MedicationRequest success. requestId="
                        + safeRequestId(request) + ", lineId="
                        + line.getPatientDispensingRequestLineId() + ", endpoint="
                        + buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_REQUEST_PATH, "/MedicationRequest"))
                        + ", payload=" + truncate(payload) + ", response=" + truncate(response));
                log(IntegrationType.HIE_MEDICATION_REQUEST, IntegrationStatus.SUCCESS,
                        buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_REQUEST_PATH, "/MedicationRequest")),
                        payload, truncate(response), "Medication request sent to HIE", start);
            }
            catch (Exception e) {
                log.error("HIE MedicationRequest failed. requestId="
                        + safeRequestId(request) + ", lineId="
                        + line.getPatientDispensingRequestLineId() + ", endpoint="
                        + buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_REQUEST_PATH, "/MedicationRequest"))
                        + ", payload=" + truncate(payload), e);
                log(IntegrationType.HIE_MEDICATION_REQUEST, IntegrationStatus.FAILED,
                        buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_REQUEST_PATH, "/MedicationRequest")),
                        payload, truncate(e.getMessage()), "Failed to send medication request to HIE", start);
            }
        }
    }

    private void sendPrescriptionSavedSms(PatientDispensingRequest request) {
        if (!Boolean.valueOf(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_ENABLED, "false"))) {
            log(IntegrationType.SMS_NOTIFICATION, IntegrationStatus.SKIPPED,
                    smsPostUrl(),
                    "", "SMS integration is disabled");
            return;
        }
        Patient patient = request == null ? null : request.getPatient();
        String phoneNumber = hieMedicationPayloadBuilder.resolvePatientPhoneNumber(patient,
                administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_PHONE_PERSON_ATTRIBUTE_TYPE_UUID, ""));
        if (isBlank(phoneNumber)) {
            log(IntegrationType.SMS_NOTIFICATION, IntegrationStatus.SKIPPED,
                    smsPostUrl(),
                    "", "Patient phone number is missing");
            return;
        }
        long start = System.currentTimeMillis();
        String message = hieMedicationPayloadBuilder.buildPrescriptionSmsMessage(request,
                facilityId("unknown"),
                resolveHospitalName());
        String configuredSender = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_SENDER, "");
        log.info("SMS send prepared. requestId=" + safeRequestId(request)
                + ", senderGlobalProperty=" + truncate(configuredSender)
                + ", phoneNumber=" + phoneNumber
                + ", message=" + truncate(message));
        try {
            String response = smsGatewayClient.send(
                    smsPostUrl(),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_USERNAME, ""),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_PASSWORD, ""),
                    configuredSender,
                    phoneNumber,
                    message,
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_CODING, "0"),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_DLR_URL, ""),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_DLR_LEVEL, "2"),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SMS_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_SMS_READ_TIMEOUT, 30000));
            log.info("SMS send success. requestId=" + safeRequestId(request) + ", phoneNumber="
                    + phoneNumber + ", postUrl="
                    + smsPostUrl()
                    + ", message=" + truncate(message) + ", response=" + truncate(response));
            log(IntegrationType.SMS_NOTIFICATION, IntegrationStatus.SUCCESS,
                    smsPostUrl(),
                    smsRequestSummary(phoneNumber, configuredSender, message), truncate(response), "Prescription SMS sent", start);
        }
        catch (Exception e) {
            log.error("SMS send failed. requestId=" + safeRequestId(request) + ", phoneNumber="
                    + phoneNumber + ", postUrl="
                    + smsPostUrl()
                    + ", message=" + truncate(message), e);
            log(IntegrationType.SMS_NOTIFICATION, IntegrationStatus.FAILED,
                    smsPostUrl(),
                    smsRequestSummary(phoneNumber, configuredSender, message), truncate(e.getMessage()),
                    "Failed to send prescription SMS", start);
        }
    }

    private boolean sendEncounter(PatientDispensingRequest request) {
        if (request == null || request.getEncounter() == null) {
            log(IntegrationType.HIE_ENCOUNTER, IntegrationStatus.SKIPPED,
                    buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_PATH, "/Encounter")),
                    "", "No encounter available to send to HIE");
            return true;
        }
        long start = System.currentTimeMillis();
        String payload = "";
        try {
            payload = hieMedicationPayloadBuilder.buildEncounter(request,
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_PATIENT_IDENTIFIER_TYPE_UUID, ""),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_SERVICE_TYPE_CODE, "57"),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_SERVICE_TYPE_DISPLAY, "Pharmacy"));
            String endpointPath = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_PATH, "/Encounter");
            String response = hieMedicationApiClient.post(
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_BASE_URL, ""),
                    endpointPath,
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_USERNAME, ""),
                    administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_PASSWORD, ""),
                    payload,
                    integerProperty(RwandaPharmacyManagementConfig.GP_HIE_CONNECT_TIMEOUT, 10000),
                    integerProperty(RwandaPharmacyManagementConfig.GP_HIE_READ_TIMEOUT, 30000));
            log.info("HIE Encounter success. requestId=" + safeRequestId(request)
                    + ", encounterId=" + request.getEncounter().getEncounterId()
                    + ", endpoint=" + buildEndpoint(endpointPath)
                    + ", payload=" + truncate(payload)
                    + ", response=" + truncate(response));
            log(IntegrationType.HIE_ENCOUNTER, IntegrationStatus.SUCCESS,
                    buildEndpoint(endpointPath), payload, truncate(response), "Encounter sent to HIE", start);
            return true;
        }
        catch (Exception e) {
            log.error("HIE Encounter failed. requestId=" + safeRequestId(request)
                    + ", encounterId=" + (request.getEncounter() == null ? "unknown" : request.getEncounter().getEncounterId())
                    + ", endpoint=" + buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_PATH, "/Encounter"))
                    + ", payload=" + truncate(payload), e);
            log(IntegrationType.HIE_ENCOUNTER, IntegrationStatus.FAILED,
                    buildEndpoint(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_PATH, "/Encounter")),
                    payload, truncate(e.getMessage()), "Failed to send encounter to HIE", start);
            return false;
        }
    }

    private boolean isHieEnabled() {
        return Boolean.valueOf(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_ENABLED, "false"))
                && !isBlank(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_BASE_URL, ""));
    }

    private void log(IntegrationType type, IntegrationStatus status, String endpoint,
                     String requestPayload, String summary) {
        log(type, status, endpoint, requestPayload, "", summary, System.currentTimeMillis());
    }

    private void log(IntegrationType type, IntegrationStatus status, String endpoint,
                     String requestPayload, String responsePayload, String summary, long start) {
        IntegrationLog log = new IntegrationLog();
        log.setIntegrationType(type);
        log.setStatus(status);
        log.setEndpoint(endpoint);
        log.setRequestPayload(truncate(requestPayload));
        log.setResponsePayload(truncate(responsePayload));
        log.setSummary(summary);
        log.setDurationMs(Math.max(0L, System.currentTimeMillis() - start));
        User currentUser = Context.getAuthenticatedUser();
        log.setCreator(currentUser);
        log.setDateCreated(new Date());
        integrationLogService.saveLog(log);
    }

    private String buildEndpoint(String path) {
        String baseUrl = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_HIE_BASE_URL, "");
        if (isBlank(baseUrl)) {
            return path == null ? "" : path;
        }
        if (isBlank(path)) {
            return baseUrl;
        }
        return baseUrl.endsWith("/") || path.startsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    }

    private Integer integerProperty(String property, int defaultValue) {
        try {
            return Integer.valueOf(administrationService.getGlobalProperty(property, String.valueOf(defaultValue)));
        }
        catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String smsRequestSummary(String phoneNumber, String sender, String message) {
        return "from=" + safeString(sender) + ", to=" + safeString(phoneNumber) + ", content=" + safeString(message)
                + ", coding=" + administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_CODING, "0")
                + ", dlr-level=" + administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_DLR_LEVEL, "2")
                + ", dlr-url=" + safeString(administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_DLR_URL, ""));
    }

    private String smsPostUrl() {
        String postUrl = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_SMS_POST_URL, "");
        return isBlank(postUrl) ? "http://41.186.44.171:1401/send" : postUrl;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private Map<String, String> templateValues(String key1, String value1, String key2, String value2) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put(key1, value1);
        values.put(key2, value2);
        return values;
    }

    private Map<String, String> templateValues(String key1, String value1, String key2, String value2,
                                               String key3, String value3) {
        Map<String, String> values = templateValues(key1, value1, key2, value2);
        values.put(key3, value3);
        return values;
    }

    private Map<String, String> templateValues(String key1, String value1, String key2, String value2,
                                               String key3, String value3, String key4, String value4) {
        Map<String, String> values = templateValues(key1, value1, key2, value2, key3, value3);
        values.put(key4, value4);
        return values;
    }

    private String renderTemplate(String template, Map<String, String> values) {
        String rendered = template == null ? "" : template;
        if (values == null) {
            return rendered;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", escapeJson(entry.getValue()));
        }
        return rendered;
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 4000 ? value : value.substring(0, 4000);
    }

    private String safeRequestId(PatientDispensingRequest request) {
        return request == null || request.getPatientDispensingRequestId() == null ? "unknown"
                : String.valueOf(request.getPatientDispensingRequestId());
    }

    private String resolveHospitalName() {
        String hospital = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL, "");
        if (!isBlank(hospital)) {
            return hospital;
        }
        return administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_HOSPITAL_NAME, "");
    }

    private String facilityId(String defaultValue) {
        String facilityId = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID, "");
        if (isBlank(facilityId)) {
            facilityId = administrationService.getGlobalProperty(RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID_LEGACY, defaultValue);
        }
        return facilityId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
