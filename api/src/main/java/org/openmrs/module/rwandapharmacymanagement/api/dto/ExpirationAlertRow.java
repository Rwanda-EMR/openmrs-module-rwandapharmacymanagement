package org.openmrs.module.rwandapharmacymanagement.api.dto;

import org.openmrs.module.rwandapharmacymanagement.model.InventoryBalance;
import org.openmrs.module.rwandapharmacymanagement.model.Item;
import org.openmrs.module.rwandapharmacymanagement.model.PharmacyLocation;

import java.math.BigDecimal;
import java.util.Date;

public class ExpirationAlertRow {

    private Item item;
    private PharmacyLocation pharmacyLocation;
    private String batchNumber;
    private Date expiryDate;
    private BigDecimal quantityOnHand = BigDecimal.ZERO;
    private Long daysToExpiry;
    private String status;

    public ExpirationAlertRow() {
    }

    public ExpirationAlertRow(InventoryBalance balance, Long daysToExpiry, String status) {
        this.item = balance.getItem();
        this.pharmacyLocation = balance.getPharmacyLocation();
        this.batchNumber = balance.getBatchNumber();
        this.expiryDate = balance.getExpiryDate();
        this.quantityOnHand = balance.getQuantityOnHand() == null ? BigDecimal.ZERO : balance.getQuantityOnHand();
        this.daysToExpiry = daysToExpiry;
        this.status = status;
    }

    public Item getItem() {
        return item;
    }

    public PharmacyLocation getPharmacyLocation() {
        return pharmacyLocation;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public Long getDaysToExpiry() {
        return daysToExpiry;
    }

    public String getStatus() {
        return status;
    }
}
