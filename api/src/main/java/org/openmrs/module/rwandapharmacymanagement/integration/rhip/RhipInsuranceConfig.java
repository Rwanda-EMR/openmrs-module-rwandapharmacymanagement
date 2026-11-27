package org.openmrs.module.rwandapharmacymanagement.integration.rhip;

import org.openmrs.api.AdministrationService;
import org.openmrs.module.rwandapharmacymanagement.RwandaPharmacyManagementConfig;

public class RhipInsuranceConfig {

    private static final String SHARED_API_PREFIX = "/insurance_integration/api/v2";
    private static final String MOHBILLING_BASE_URL = "mohbilling.rhip.baseUrl";
    private static final String MOHBILLING_API_KEY = "mohbilling.rhipVoucher.apiKey";
    private static final String MOHBILLING_API_ORIGIN = "mohbilling.rhipVoucher.apiOrigin";
    private static final String RWANDAEMR_BASE_URL = "rwandaemr.insuranceIntegration.baseUrl";
    private static final String RWANDAEMR_API_KEY = "rwandaemr.insuranceEligibility.apiKey";
    private static final String RWANDAEMR_API_ORIGIN = "rwandaemr.insuranceEligibility.apiOrigin";

    private AdministrationService administrationService;

    public void setAdministrationService(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    public boolean isEnabled() {
        return Boolean.valueOf(property(RwandaPharmacyManagementConfig.GP_RHIP_ENABLED, "false"))
                && !isBlank(baseUrl());
    }

    public String inpatientPrescriptionUrl() {
        return joinUrl(baseUrl(), property(RwandaPharmacyManagementConfig.GP_RHIP_INPATIENT_PRESCRIPTION_PATH,
                "/practitioner/inpatient-prescription"));
    }

    public String visitDispenseUrl() {
        return joinUrl(baseUrl(), property(RwandaPharmacyManagementConfig.GP_RHIP_VISIT_DISPENSE_PATH,
                "/visit-dispense"));
    }

    public String medicineDeliveryUrl() {
        return joinUrl(baseUrl(), property(RwandaPharmacyManagementConfig.GP_RHIP_MEDICINE_DELIVERY_PATH,
                "/practitioner/medicines/delivery"));
    }

    /**
     * Deliveries are sent only when RHIP is enabled and this switch is on, so the delivery price can be turned off
     * on its own while the other MMI pharmacy calls keep running
     */
    public boolean isMedicineDeliveryEnabled() {
        return isEnabled() && Boolean.valueOf(property(RwandaPharmacyManagementConfig.GP_RHIP_MEDICINE_DELIVERY_ENABLED, "true"));
    }

    /**
     * The RHIP connection settings of the eligibility check are used for every RHIP call; mohbilling's are only read
     * when they are empty
     */
    public String apiKey() {
        return firstNonBlank(property(RWANDAEMR_API_KEY, null),
                property(MOHBILLING_API_KEY, null));
    }

    public String apiOrigin() {
        return firstNonBlank(property(RWANDAEMR_API_ORIGIN, null),
                property(MOHBILLING_API_ORIGIN, null));
    }

    public Integer connectionTimeout() {
        return integerProperty(RwandaPharmacyManagementConfig.GP_RHIP_CONNECT_TIMEOUT, 15000);
    }

    public Integer readTimeout() {
        return integerProperty(RwandaPharmacyManagementConfig.GP_RHIP_READ_TIMEOUT, 30000);
    }

    private String baseUrl() {
        return firstNonBlank(property(RWANDAEMR_BASE_URL, null),
                property(MOHBILLING_BASE_URL, null));
    }

    private String joinUrl(String baseUrl, String endpointPath) {
        if (!isBlank(endpointPath) && (endpointPath.trim().startsWith("http://")
                || endpointPath.trim().startsWith("https://"))) {
            return endpointPath.trim();
        }
        if (isBlank(baseUrl)) {
            return "";
        }
        String normalizedBase = baseUrl.trim();
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        if (!normalizedBase.endsWith(SHARED_API_PREFIX)) {
            normalizedBase = normalizedBase + SHARED_API_PREFIX;
        }
        String normalizedPath = isBlank(endpointPath) ? "" : endpointPath.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return normalizedBase + normalizedPath;
    }

    private Integer integerProperty(String property, int defaultValue) {
        try {
            return Integer.valueOf(property(property, String.valueOf(defaultValue)));
        }
        catch (Exception ignored) {
            return Integer.valueOf(defaultValue);
        }
    }

    private String property(String property, String defaultValue) {
        return administrationService == null ? defaultValue : administrationService.getGlobalProperty(property, defaultValue);
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        return isBlank(second) ? null : second.trim();
    }

    private String firstNonBlank(String first, String second, String third) {
        if (!isBlank(first)) {
            return first.trim();
        }
        if (!isBlank(second)) {
            return second.trim();
        }
        return isBlank(third) ? null : third.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
