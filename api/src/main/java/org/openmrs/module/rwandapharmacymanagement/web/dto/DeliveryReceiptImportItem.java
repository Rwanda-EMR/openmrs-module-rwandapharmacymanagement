package org.openmrs.module.rwandapharmacymanagement.web.dto;

import java.math.BigDecimal;

public class DeliveryReceiptImportItem {

    private String npcCode;
    private Integer productMasterItemId;
    private String program;
    private String description;
    private BigDecimal deliveryQuantity;
    private String units;
    private String batch;
    private String expiryDate;
    private String itemNo;
    private BigDecimal acceptedQuantity;
    private String rejectionReasonCode;
    private String rejectionReasonDetail;
    private String notes;

    public String getNpcCode() {
        return npcCode;
    }

    public void setNpcCode(String npcCode) {
        this.npcCode = npcCode;
    }

    public Integer getProductMasterItemId() {
        return productMasterItemId;
    }

    public void setProductMasterItemId(Integer productMasterItemId) {
        this.productMasterItemId = productMasterItemId;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDeliveryQuantity() {
        return deliveryQuantity;
    }

    public void setDeliveryQuantity(BigDecimal deliveryQuantity) {
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

    public BigDecimal getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(BigDecimal acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public String getRejectionReasonCode() {
        return rejectionReasonCode;
    }

    public void setRejectionReasonCode(String rejectionReasonCode) {
        this.rejectionReasonCode = rejectionReasonCode;
    }

    public String getRejectionReasonDetail() {
        return rejectionReasonDetail;
    }

    public void setRejectionReasonDetail(String rejectionReasonDetail) {
        this.rejectionReasonDetail = rejectionReasonDetail;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
