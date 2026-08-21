package org.openmrs.module.rwandapharmacymanagement.model;

import java.math.BigDecimal;
import java.util.Date;

public class DepartmentStockReturnLine extends BaseTrackedEntity {

    private Integer departmentStockReturnLineId;
    private DepartmentStockReturn departmentStockReturn;
    private Item item;
    private String batchNumber;
    private Date expiryDate;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    public Integer getDepartmentStockReturnLineId() {
        return departmentStockReturnLineId;
    }

    public void setDepartmentStockReturnLineId(Integer departmentStockReturnLineId) {
        this.departmentStockReturnLineId = departmentStockReturnLineId;
    }

    public DepartmentStockReturn getDepartmentStockReturn() {
        return departmentStockReturn;
    }

    public void setDepartmentStockReturn(DepartmentStockReturn departmentStockReturn) {
        this.departmentStockReturn = departmentStockReturn;
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
