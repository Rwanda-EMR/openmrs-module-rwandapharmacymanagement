package org.openmrs.module.rwandapharmacymanagement.integration.supplier.dto;

public class SupplierDeliveryDetailsItem {

    private String npcCode;
    private String description;
    private String deliveryQuantity;
    private String units;
    private String batch;
    private String expiryDate;
    private String itemNo;

    public String getNpcCode() {
        return npcCode;
    }

    public void setNpcCode(String npcCode) {
        this.npcCode = npcCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeliveryQuantity() {
        return deliveryQuantity;
    }

    public void setDeliveryQuantity(String deliveryQuantity) {
        this.deliveryQuantity = deliveryQuantity;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }
}
