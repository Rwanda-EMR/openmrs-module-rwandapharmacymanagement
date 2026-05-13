package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;
import java.util.Date;

public class DispenseRecordLine extends BaseTrackedEntity {

    private Integer dispenseRecordLineId;
    private DispenseRecord dispenseRecord;
    private Item item;
    private String batchNumber;
    private Date expiryDate;
    private BigDecimal quantityDispensed;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal lineTotal;

    public Integer getDispenseRecordLineId() {
        return dispenseRecordLineId;
    }

    public void setDispenseRecordLineId(Integer dispenseRecordLineId) {
        this.dispenseRecordLineId = dispenseRecordLineId;
    }

    public DispenseRecord getDispenseRecord() {
        return dispenseRecord;
    }

    public void setDispenseRecord(DispenseRecord dispenseRecord) {
        this.dispenseRecord = dispenseRecord;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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

    public BigDecimal getQuantityDispensed() {
        return quantityDispensed;
    }

    public void setQuantityDispensed(BigDecimal quantityDispensed) {
        this.quantityDispensed = quantityDispensed;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
