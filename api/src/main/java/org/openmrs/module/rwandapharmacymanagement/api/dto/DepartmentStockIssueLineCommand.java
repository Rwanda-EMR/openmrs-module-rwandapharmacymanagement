package org.openmrs.module.rwandapharmacymanagement.api.dto;

import java.math.BigDecimal;

public class DepartmentStockIssueLineCommand {

    private Integer itemId;
    private BigDecimal quantity;
    private String batchNumber;

    public DepartmentStockIssueLineCommand() {
    }

    public DepartmentStockIssueLineCommand(Integer itemId, BigDecimal quantity, String batchNumber) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.batchNumber = batchNumber;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }
}
