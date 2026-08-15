package org.openmrs.module.rwandapharmacymanagement.api.dto;

import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.math.BigDecimal;

public class StockoutAlertRow {

    private Item item;
    private PharmacyLocation pharmacyLocation;
    private BigDecimal currentQuantity = BigDecimal.ZERO;
    private BigDecimal consumedQuantity = BigDecimal.ZERO;
    private BigDecimal averageDailyConsumption = BigDecimal.ZERO;
    private BigDecimal daysOfStock;
    private String status;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public PharmacyLocation getPharmacyLocation() {
        return pharmacyLocation;
    }

    public void setPharmacyLocation(PharmacyLocation pharmacyLocation) {
        this.pharmacyLocation = pharmacyLocation;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity == null ? BigDecimal.ZERO : currentQuantity;
    }

    public BigDecimal getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(BigDecimal consumedQuantity) {
        this.consumedQuantity = consumedQuantity == null ? BigDecimal.ZERO : consumedQuantity;
    }

    public BigDecimal getAverageDailyConsumption() {
        return averageDailyConsumption;
    }

    public void setAverageDailyConsumption(BigDecimal averageDailyConsumption) {
        this.averageDailyConsumption = averageDailyConsumption == null ? BigDecimal.ZERO : averageDailyConsumption;
    }

    public BigDecimal getDaysOfStock() {
        return daysOfStock;
    }

    public void setDaysOfStock(BigDecimal daysOfStock) {
        this.daysOfStock = daysOfStock;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
