package org.openmrs.module.rwandapharmacymanagement.model;

import org.openmrs.User;
import org.openmrs.module.rwandapharmacymanagement.model.enums.StockTransactionType;

import java.math.BigDecimal;
import java.util.Date;

public class StockTransaction extends BaseTrackedEntity {

    private Integer stockTransactionId;
    private Item item;
    private PharmacyLocation pharmacyLocation;
    private StockTransactionType transactionType;
    private String batchNumber;
    private Date expiryDate;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String referenceNumber;
    private String remarks;
    private User performedBy;

    public Integer getStockTransactionId() {
        return stockTransactionId;
    }

    public void setStockTransactionId(Integer stockTransactionId) {
        this.stockTransactionId = stockTransactionId;
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

    public StockTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(StockTransactionType transactionType) {
        this.transactionType = transactionType;
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

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
    }
}
