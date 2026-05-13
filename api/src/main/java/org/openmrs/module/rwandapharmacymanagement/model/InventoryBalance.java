package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;
import java.util.Date;

public class InventoryBalance extends BaseTrackedEntity {

    private Integer inventoryBalanceId;
    private Item item;
    private PharmacyLocation pharmacyLocation;
    private String batchNumber;
    private Date expiryDate;
    private BigDecimal quantityOnHand;
    private BigDecimal unitPrice;

    public Integer getInventoryBalanceId() {
        return inventoryBalanceId;
    }

    public void setInventoryBalanceId(Integer inventoryBalanceId) {
        this.inventoryBalanceId = inventoryBalanceId;
    }

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

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
