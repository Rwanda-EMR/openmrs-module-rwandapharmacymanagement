package org.openmrs.module.rwandapharmacymanagement.api.dto;

public class AlertFilter {

    private Integer pharmacyLocationId;
    private Integer itemCategoryId;
    private String searchTerm;
    private String expiryStatus;
    private String stockStatus;
    private Integer expiryAlertDays;
    private Integer daysOfStockAlertDays;
    private Integer consumptionLookbackDays;

    public Integer getPharmacyLocationId() {
        return pharmacyLocationId;
    }

    public void setPharmacyLocationId(Integer pharmacyLocationId) {
        this.pharmacyLocationId = pharmacyLocationId;
    }

    public Integer getItemCategoryId() {
        return itemCategoryId;
    }

    public void setItemCategoryId(Integer itemCategoryId) {
        this.itemCategoryId = itemCategoryId;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getExpiryStatus() {
        return expiryStatus;
    }

    public void setExpiryStatus(String expiryStatus) {
        this.expiryStatus = expiryStatus;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public Integer getExpiryAlertDays() {
        return expiryAlertDays;
    }

    public void setExpiryAlertDays(Integer expiryAlertDays) {
        this.expiryAlertDays = expiryAlertDays;
    }

    public Integer getDaysOfStockAlertDays() {
        return daysOfStockAlertDays;
    }

    public void setDaysOfStockAlertDays(Integer daysOfStockAlertDays) {
        this.daysOfStockAlertDays = daysOfStockAlertDays;
    }

    public Integer getConsumptionLookbackDays() {
        return consumptionLookbackDays;
    }

    public void setConsumptionLookbackDays(Integer consumptionLookbackDays) {
        this.consumptionLookbackDays = consumptionLookbackDays;
    }
}
