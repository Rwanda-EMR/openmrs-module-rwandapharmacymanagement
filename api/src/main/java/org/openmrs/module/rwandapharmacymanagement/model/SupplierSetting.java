package org.openmrs.module.rwandapharmacymanagement.model;

public class SupplierSetting extends BaseTrackedEntity {

    private Integer supplierSettingId;
    private String baseUrl;
    private String username;
    private String password;
    private String facilityId;
    private Integer connectionTimeout;
    private Integer readTimeout;
    private Boolean integrationEnabled = Boolean.FALSE;

    public Integer getSupplierSettingId() {
        return supplierSettingId;
    }

    public void setSupplierSettingId(Integer supplierSettingId) {
        this.supplierSettingId = supplierSettingId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Boolean getIntegrationEnabled() {
        return integrationEnabled;
    }

    public void setIntegrationEnabled(Boolean integrationEnabled) {
        this.integrationEnabled = integrationEnabled;
    }
}
