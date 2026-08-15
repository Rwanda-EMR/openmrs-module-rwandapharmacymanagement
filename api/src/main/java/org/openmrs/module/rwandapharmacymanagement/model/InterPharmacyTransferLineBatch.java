package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;
import java.util.Date;

public class InterPharmacyTransferLineBatch extends BaseTrackedEntity {

    private Integer interPharmacyTransferLineBatchId;
    private InterPharmacyTransferLine interPharmacyTransferLine;
    private String batchNumber;
    private Date expiryDate;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    public Integer getInterPharmacyTransferLineBatchId() {
        return interPharmacyTransferLineBatchId;
    }

    public void setInterPharmacyTransferLineBatchId(Integer interPharmacyTransferLineBatchId) {
        this.interPharmacyTransferLineBatchId = interPharmacyTransferLineBatchId;
    }

    public InterPharmacyTransferLine getInterPharmacyTransferLine() {
        return interPharmacyTransferLine;
    }

    public void setInterPharmacyTransferLine(InterPharmacyTransferLine interPharmacyTransferLine) {
        this.interPharmacyTransferLine = interPharmacyTransferLine;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
