package org.openmrs.module.rwandapharmacymanagement;

public final class RwandaPharmacyManagementConfig {

    public static final String MODULE_ID = "rwandapharmacymanagement";
    public static final String MODULE_NAME = "Rwanda Pharmacy Management";

    public static final String GP_SUPPLIER_BASE_URL = MODULE_ID + ".supplier.baseUrl";
    public static final String GP_SUPPLIER_USERNAME = MODULE_ID + ".supplier.username";
    public static final String GP_SUPPLIER_PASSWORD = MODULE_ID + ".supplier.password";
    public static final String GP_PURCHASE_ORDER_FACILITY_ID = MODULE_ID + ".facilityId";
    public static final String GP_PURCHASE_ORDER_FACILITY_ID_LEGACY = "rwandapharmancymanagement.facilityId";
    public static final String GP_PURCHASE_ORDER_HOSPITAL = MODULE_ID + ".purchaseOrder.hospital";
    public static final String GP_PURCHASE_ORDER_HOSPITAL_NAME = MODULE_ID + ".purchaseOrder.hospitalName";
    public static final String GP_SUPPLIER_CONNECT_TIMEOUT = MODULE_ID + ".supplier.connectionTimeout";
    public static final String GP_SUPPLIER_READ_TIMEOUT = MODULE_ID + ".supplier.readTimeout";
    public static final String GP_SUPPLIER_ENABLED = MODULE_ID + ".supplier.enabled";
    public static final String GP_RMS_DELIVERY_FETCH_SCHEDULER_ENABLED = MODULE_ID + ".supplier.deliveryFetchSchedulerEnabled";
    public static final String GP_RMS_DELIVERY_FETCH_SCHEDULER_INTERVAL_SECONDS = MODULE_ID + ".supplier.deliveryFetchSchedulerIntervalSeconds";
    public static final String GP_SMTP_HOST = MODULE_ID + ".smtp.host";
    public static final String GP_SMTP_PORT = MODULE_ID + ".smtp.port";
    public static final String GP_SMTP_USERNAME = MODULE_ID + ".smtp.username";
    public static final String GP_SMTP_PASSWORD = MODULE_ID + ".smtp.password";
    public static final String GP_SMTP_FROM = MODULE_ID + ".smtp.from";
    public static final String GP_SMTP_AUTH = MODULE_ID + ".smtp.auth";
    public static final String GP_SMTP_STARTTLS = MODULE_ID + ".smtp.starttls";
    public static final String GP_EXPIRY_ALERT_DAYS = MODULE_ID + ".alerts.expiryDays";
    public static final String GP_DISPENSING_MARKUP_PERCENTAGE = MODULE_ID + ".pricing.dispensingMarkupPercentage";
    public static final String GP_PRESCRIPTION_PAYMENT_BILL_MODE = MODULE_ID + ".prescription.paymentBillMode";
    public static final String GP_PRESCRIPTION_FREQUENCY_CONCEPT_SET = MODULE_ID + ".prescription.frequencyConceptSet";
    public static final String GP_LAB_SERVICE_CONCEPT_SET = MODULE_ID + ".lab.serviceConceptSet";
    public static final String GP_HIE_ENABLED = MODULE_ID + ".hie.enabled";
    public static final String GP_HIE_BASE_URL = MODULE_ID + ".hie.baseUrl";
    public static final String GP_HIE_USERNAME = MODULE_ID + ".hie.username";
    public static final String GP_HIE_PASSWORD = MODULE_ID + ".hie.password";
    public static final String GP_HIE_ENCOUNTER_PATH = MODULE_ID + ".hie.encounterPath";
    public static final String GP_HIE_ENCOUNTER_SERVICE_TYPE_CODE = MODULE_ID + ".hie.encounterServiceTypeCode";
    public static final String GP_HIE_ENCOUNTER_SERVICE_TYPE_DISPLAY = MODULE_ID + ".hie.encounterServiceTypeDisplay";
    public static final String GP_HIE_GROUP_IDENTIFIER_SYSTEM = MODULE_ID + ".hie.groupIdentifierSystem";
    public static final String GP_HIE_MEDICATION_REQUEST_PATH = MODULE_ID + ".hie.medicationRequestPath";
    public static final String GP_HIE_MEDICATION_DISPENSE_PATH = MODULE_ID + ".hie.medicationDispensePath";
    public static final String GP_HIE_PATIENT_IDENTIFIER_TYPE_UUID = MODULE_ID + ".hie.patientIdentifierTypeUuid";
    public static final String GP_HIE_MEDICATION_CODE_SYSTEM = MODULE_ID + ".hie.medicationCodeSystem";
    public static final String GP_HIE_CONNECT_TIMEOUT = MODULE_ID + ".hie.connectionTimeout";
    public static final String GP_HIE_READ_TIMEOUT = MODULE_ID + ".hie.readTimeout";
    public static final String GP_SMS_ENABLED = MODULE_ID + ".sms.enabled";
    public static final String GP_SMS_AUTH_URL = MODULE_ID + ".sms.authorizationUrl";
    public static final String GP_SMS_USERNAME = MODULE_ID + ".sms.username";
    public static final String GP_SMS_PASSWORD = MODULE_ID + ".sms.password";
    public static final String GP_SMS_POST_URL = MODULE_ID + ".sms.postUrl";
    public static final String GP_SMS_SENDER = MODULE_ID + ".sms.sender";
    public static final String GP_SMS_CODING = MODULE_ID + ".sms.coding";
    public static final String GP_SMS_DLR_URL = MODULE_ID + ".sms.dlrUrl";
    public static final String GP_SMS_DLR_LEVEL = MODULE_ID + ".sms.dlrLevel";
    public static final String GP_SMS_PHONE_PERSON_ATTRIBUTE_TYPE_UUID = MODULE_ID + ".sms.phonePersonAttributeTypeUuid";
    public static final String GP_SMS_CONNECT_TIMEOUT = MODULE_ID + ".sms.connectionTimeout";
    public static final String GP_SMS_READ_TIMEOUT = MODULE_ID + ".sms.readTimeout";
    public static final String GP_SMS_AUTH_PAYLOAD_TEMPLATE = MODULE_ID + ".sms.authPayloadTemplate";
    public static final String GP_SMS_POST_PAYLOAD_TEMPLATE = MODULE_ID + ".sms.postPayloadTemplate";

    public static final String PRIVILEGE_DASHBOARD = "App: rwandapharmacymanagement.dashboard";
    public static final String PRIVILEGE_MANAGE_SETTINGS = "Task: rwandapharmacymanagement.manageSettings";
    public static final String PRIVILEGE_SUBMIT_SUPPLIER_ORDERS = "Task: rwandapharmacymanagement.submitSupplierOrders";
    public static final String PRIVILEGE_APPROVE_PURCHASE_ORDERS = "Task: rwandapharmacymanagement.approvePurchaseOrders";
    public static final String PRIVILEGE_TRANSFER_STOCK = "Task: rwandapharmacymanagement.transferStock";
    public static final String PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER = "Task: rwandapharmacymanagement.approveMainToDispensingTransfer";

    private RwandaPharmacyManagementConfig() {
    }
}
