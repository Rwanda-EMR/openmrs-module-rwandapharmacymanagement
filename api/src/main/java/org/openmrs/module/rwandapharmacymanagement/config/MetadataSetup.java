package org.openmrs.module.rwandapharmacymanagement.config;

import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandapharmacymanagement.api.DepartmentBulkRequestService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class MetadataSetup implements Setup {

    private static final Set<String> PRIVILEGES = new LinkedHashSet<String>(Arrays.asList(
            RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
            RwandaPharmacyManagementConfig.PRIVILEGE_CREATE_PRESCRIPTION,
            RwandaPharmacyManagementConfig.PRIVILEGE_ORDER_CONSUMABLES,
            "Task: rwandapharmacymanagement.manageItems",
            "Task: rwandapharmacymanagement.managePharmacies",
            "Task: rwandapharmacymanagement.manageSupplierSettings",
            "Task: rwandapharmacymanagement.syncSupplierProducts",
            "Task: rwandapharmacymanagement.syncSupplierPrices",
            RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS,
            RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS,
            "Task: rwandapharmacymanagement.receiveDeliveries",
            "Task: rwandapharmacymanagement.viewStock",
            RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK,
            RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER,
            RwandaPharmacyManagementConfig.PRIVILEGE_DEPARTMENT_STORE,
            RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK,
            RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PAYMENT_EXCEPTION,
            "Task: rwandapharmacymanagement.viewPatientQueue",
            "Task: rwandapharmacymanagement.dispenseWithOrder",
            "Task: rwandapharmacymanagement.dispenseWithoutOrder",
            "Task: rwandapharmacymanagement.manageClinicalSafetyRules",
            "Task: rwandapharmacymanagement.viewIntegrationLogs",
            RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS
    ));

    @Override
    public void initialize() {
        ensurePrivileges();
        ensureRoles();
        ensureGlobalProperties();
    }

    @Override
    public void teardown() {
        // Intentionally no-op. Metadata is retained across module restarts.
    }

    private void ensurePrivileges() {
        UserService userService = Context.getUserService();
        for (String privilegeName : PRIVILEGES) {
            if (userService.getPrivilege(privilegeName) == null) {
                Privilege privilege = new Privilege(privilegeName, "Auto-created by " + RwandaPharmacyManagementConfig.MODULE_NAME);
                userService.savePrivilege(privilege);
            }
        }
    }

    private void ensureRoles() {
        UserService userService = Context.getUserService();
        saveRole(userService, "Rwanda Pharmacy Pharmacist",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock",
                "Task: rwandapharmacymanagement.viewPatientQueue",
                "Task: rwandapharmacymanagement.dispenseWithOrder",
                "Task: rwandapharmacymanagement.dispenseWithoutOrder",
                RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PAYMENT_EXCEPTION);
        saveRole(userService, "Rwanda Pharmacy Dispenser",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock",
                "Task: rwandapharmacymanagement.viewPatientQueue",
                "Task: rwandapharmacymanagement.dispenseWithOrder",
                "Task: rwandapharmacymanagement.dispenseWithoutOrder");
        saveRole(userService, "Rwanda Pharmacy Store Manager",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock",
                "Task: rwandapharmacymanagement.receiveDeliveries",
                RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS);
        saveRole(userService, "Rwanda Pharmacy Main Store Manager",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock",
                "Task: rwandapharmacymanagement.receiveDeliveries",
                RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS);
        saveRole(userService, "Rwanda Pharmacy Dispensing Store Manager",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock",
                "Task: rwandapharmacymanagement.viewPatientQueue",
                "Task: rwandapharmacymanagement.dispenseWithOrder",
                "Task: rwandapharmacymanagement.dispenseWithoutOrder",
                RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PAYMENT_EXCEPTION);
        saveRole(userService, "Rwanda Pharmacy Purchase Order Officer",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock",
                RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS);
        saveRole(userService, "Rwanda Pharmacy Stock Receiver",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock",
                "Task: rwandapharmacymanagement.receiveDeliveries");
        saveRole(userService, "Rwanda Pharmacy Department Store",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                RwandaPharmacyManagementConfig.PRIVILEGE_DEPARTMENT_STORE,
                "Task: rwandapharmacymanagement.viewStock");
        saveRole(userService, "Rwanda Pharmacy Reports Viewer",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock");
        saveRole(userService, "Rwanda Pharmacy Head of Finance",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewStock",
                RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER);
        saveRole(userService, "Rwanda Pharmacy Health Facility Leader",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS,
                "Task: rwandapharmacymanagement.viewStock");
        saveRole(userService, "Rwanda Pharmacy Doctor",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.viewPatientQueue",
                RwandaPharmacyManagementConfig.PRIVILEGE_CREATE_PRESCRIPTION,
                RwandaPharmacyManagementConfig.PRIVILEGE_ORDER_CONSUMABLES);
        saveRole(userService, "Rwanda Pharmacy Nurse",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                RwandaPharmacyManagementConfig.PRIVILEGE_ORDER_CONSUMABLES);
        saveRole(userService, "Rwanda Pharmacy Administrator",
                RwandaPharmacyManagementConfig.PRIVILEGE_DASHBOARD,
                "Task: rwandapharmacymanagement.manageItems",
                "Task: rwandapharmacymanagement.managePharmacies",
                "Task: rwandapharmacymanagement.manageSupplierSettings",
                "Task: rwandapharmacymanagement.syncSupplierProducts",
                "Task: rwandapharmacymanagement.syncSupplierPrices",
                RwandaPharmacyManagementConfig.PRIVILEGE_SUBMIT_SUPPLIER_ORDERS,
                RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PURCHASE_ORDERS,
                "Task: rwandapharmacymanagement.receiveDeliveries",
                "Task: rwandapharmacymanagement.viewStock",
                RwandaPharmacyManagementConfig.PRIVILEGE_TRANSFER_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_MAIN_TO_DISPENSING_TRANSFER,
                RwandaPharmacyManagementConfig.PRIVILEGE_DEPARTMENT_STORE,
                RwandaPharmacyManagementConfig.PRIVILEGE_ADJUST_STOCK,
                RwandaPharmacyManagementConfig.PRIVILEGE_APPROVE_PAYMENT_EXCEPTION,
                "Task: rwandapharmacymanagement.viewPatientQueue",
                "Task: rwandapharmacymanagement.dispenseWithOrder",
                "Task: rwandapharmacymanagement.dispenseWithoutOrder",
                RwandaPharmacyManagementConfig.PRIVILEGE_CREATE_PRESCRIPTION,
                RwandaPharmacyManagementConfig.PRIVILEGE_ORDER_CONSUMABLES,
                "Task: rwandapharmacymanagement.manageClinicalSafetyRules",
                "Task: rwandapharmacymanagement.viewIntegrationLogs",
                RwandaPharmacyManagementConfig.PRIVILEGE_MANAGE_SETTINGS);
    }

    private void saveRole(UserService userService, String name, String... privileges) {
        Role role = userService.getRole(name);
        if (role == null) {
            role = new Role(name);
        }
        role.setDescription("Auto-created by " + RwandaPharmacyManagementConfig.MODULE_NAME);
        for (String privilege : privileges) {
            Privilege existing = userService.getPrivilege(privilege);
            if (existing != null) {
                role.addPrivilege(existing);
            }
        }
        userService.saveRole(role);
    }

    private void ensureGlobalProperties() {
        AdministrationService administrationService = Context.getAdministrationService();
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SUPPLIER_BASE_URL, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SUPPLIER_USERNAME, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SUPPLIER_PASSWORD, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID, "1660");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_PURCHASE_ORDER_FACILITY_ID_LEGACY, "1660");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SUPPLIER_CONNECT_TIMEOUT, "10000");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SUPPLIER_READ_TIMEOUT, "30000");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SUPPLIER_ENABLED, "false");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_RMS_DELIVERY_FETCH_SCHEDULER_ENABLED, "false");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_RMS_DELIVERY_FETCH_SCHEDULER_INTERVAL_SECONDS, "3600");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMTP_HOST, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMTP_PORT, "587");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMTP_USERNAME, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMTP_PASSWORD, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMTP_FROM, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMTP_AUTH, "true");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMTP_STARTTLS, "true");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_EXPIRY_ALERT_DAYS, "90");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_DISPENSING_MARKUP_PERCENTAGE, "0");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_PRESCRIPTION_PAYMENT_BILL_MODE, "latest");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_ENABLED, "false");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_BASE_URL, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_USERNAME, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_PASSWORD, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_PATH, "/Encounter");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_SERVICE_TYPE_CODE, "57");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_ENCOUNTER_SERVICE_TYPE_DISPLAY, "Pharmacy");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_GROUP_IDENTIFIER_SYSTEM,
                "http://openmrs.org/rwanda-pharmacy-management/prescription-order-number");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_SOURCE_SYSTEM, "eBuzima");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_REQUEST_PATH, "/MedicationRequest");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_DISPENSE_PATH, "/MedicationDispense");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_PATIENT_IDENTIFIER_TYPE_UUID, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_MEDICATION_CODE_SYSTEM, "http://openmrs.org/rwanda-pharmacy-management/item-code");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_CONNECT_TIMEOUT, "10000");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_READ_TIMEOUT, "30000");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_ENABLED, "true");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_RETRY_SCHEDULER_INTERVAL_SECONDS, "1800");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_HIE_RETRY_BATCH_SIZE, "50");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_ENABLED, "false");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_AUTH_URL, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_USERNAME, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_PASSWORD, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_POST_URL, "http://41.186.44.171:1401/send");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_SENDER, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_CODING, "0");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_DLR_URL, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_DLR_LEVEL, "2");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_PHONE_PERSON_ATTRIBUTE_TYPE_UUID, "");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_CONNECT_TIMEOUT, "10000");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_READ_TIMEOUT, "30000");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_AUTH_PAYLOAD_TEMPLATE, "{\"username\":\"{{username}}\",\"password\":\"{{password}}\"}");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_POST_PAYLOAD_TEMPLATE, "{\"sender\":\"{{sender}}\",\"recipient\":\"{{phoneNumber}}\",\"message\":\"{{message}}\"}");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_ENABLED, "true");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_RETRY_SCHEDULER_INTERVAL_SECONDS, "1800");
        saveGlobalProperty(administrationService, RwandaPharmacyManagementConfig.GP_SMS_RETRY_BATCH_SIZE, "50");
        saveGlobalProperty(administrationService, DepartmentBulkRequestService.GP_WORKFLOW_MODE,
                DepartmentBulkRequestService.MODE_PERIODIC_ACCOUNTABILITY);
    }

    private void saveGlobalProperty(AdministrationService administrationService, String property, String defaultValue) {
        if (administrationService.getGlobalPropertyObject(property) == null) {
            administrationService.saveGlobalProperty(new org.openmrs.GlobalProperty(property, defaultValue));
        }
    }
}
