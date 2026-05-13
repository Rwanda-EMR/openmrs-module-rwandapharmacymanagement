package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;
import java.util.Date;

public class StockAdjustmentLine extends BaseTrackedEntity {

    private Integer stockAdjustmentLineId;
    private StockAdjustment stockAdjustment;
    private Item item;
    private String batchNumber;
    private Date expiryDate;
    private BigDecimal quantity;
    private BigDecimal systemQuantity;

    public Integer getStockAdjustmentLineId() {
        return stockAdjustmentLineId;
    }

    public void setStockAdjustmentLineId(Integer stockAdjustmentLineId) {
        this.stockAdjustmentLineId = stockAdjustmentLineId;
    }

    public StockAdjustment getStockAdjustment() {
        return stockAdjustment;
    }

    public void setStockAdjustment(StockAdjustment stockAdjustment) {
        this.stockAdjustment = stockAdjustment;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSystemQuantity() {
        return systemQuantity;
    }

    public void setSystemQuantity(BigDecimal systemQuantity) {
        this.systemQuantity = systemQuantity;
    }
}
